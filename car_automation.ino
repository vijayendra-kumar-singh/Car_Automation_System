#include <ESP8266WiFi.h>
#include <ArduinoJson.h>
WiFiServer server(80);

String apiKey = "MGDD3YC7UCPAE3BW";

int SensorPin = 17;                                                // Pin To Detect Gas
int AlarmPin = 5;                                                 //  Pin using For Alarm
int WindowPin = 12;                                              //  Pin Using For Lowering Window

int sensorThres = 100;

char   host[] = "api.thingspeak.com";                         // Host For Your data
String channelID = "454941";                                 // Things Speak Channel(Car Window) ID
const int httpPort = 80;                                    //  Http Port For Send Data

const char *ssid     = "Dilip";                            // Wifi Name
const char *password = "22222222";                        //  Wifi Password
WiFiClient client;

int systemStatus = 1;                                   // 1 means Your whole system is working
int windowStatus = 0;                                  // 0 means Window is Currently Closed
int buzzerStatus = 1;                                 //  1 means Buzzer will Beep.

#define GAS_LPG 0
#define GAS_CO 1
#define GAS_SMOKE 2

int RL_VALUE = 5;
float Ro = 10;
float RO_CLEAN_AIR_FACTOR = 9.83;
int CALIBARAION_SAMPLE_TIMES = 50;
int CALIBRATION_SAMPLE_INTERVAL = 500;
int READ_SAMPLE_INTERVAL = 50;
int READ_SAMPLE_TIMES = 5;

float LPGCurve[3]  =  {2.3, 0.21, -0.47};
float COCurve[3]  =  {2.3, 0.72, -0.34};
float SmokeCurve[3] = {2.3, 0.53, -0.44};

void setup() {

  Serial.begin(115200);

  pinMode(SensorPin, INPUT);
  pinMode(AlarmPin, OUTPUT);
  pinMode(WindowPin, OUTPUT);

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    Serial.println("----------------------Waiting for network-------------------------");
    delay(500);
  }

  Serial.println();
  Serial.println(ssid);
  Serial.print("..........");
  Serial.println();
  WiFi.begin(ssid, password);
  Serial.print(".");

  Serial.println("WiFi connected");
  Serial.println();

  Serial.println("---------------- calibrating ------------------");
  Ro = MQCalibration(SensorPin);
  Serial.println("---------------- calibration done ------------------");
}

float MQCalibration(int SensorPin)
{
  int i;
  float val = 0;

  for (i = 0; i < CALIBARAION_SAMPLE_TIMES; i++) {
    val += MQResistanceCalculation(analogRead(SensorPin));
//    delay(CALIBRATION_SAMPLE_INTERVAL);
  }
  val = val / CALIBARAION_SAMPLE_TIMES;
  val = val / RO_CLEAN_AIR_FACTOR;
  return val;
}

float MQResistanceCalculation(int raw_adc)
{
  return ( ((float)RL_VALUE * (1023 - raw_adc) / raw_adc));
}

float MQGetGasPercentage(float rs_ro_ratio, int gas_id)
{
  if ( gas_id == GAS_LPG ) {
    return MQGetPercentage(rs_ro_ratio, LPGCurve);
  } else if ( gas_id == GAS_CO ) {
    return MQGetPercentage(rs_ro_ratio, COCurve);
  } else if ( gas_id == GAS_SMOKE ) {
    return MQGetPercentage(rs_ro_ratio, SmokeCurve);
  }
  return 0;
}

float MQRead(int mq_pin)
{
  int i;
  float rs = 0;

  for (i = 0; i < READ_SAMPLE_TIMES; i++) {
    rs += MQResistanceCalculation(analogRead(SensorPin));
    delay(READ_SAMPLE_INTERVAL);
  }
  rs = rs / READ_SAMPLE_TIMES;
  return rs;
}

float  MQGetPercentage(float rs_ro_ratio, float *pcurve)
{
  return (pow(10, ( ((log(rs_ro_ratio) - pcurve[1]) / pcurve[2]) + pcurve[0])));
}

void loop() {

  float LPG = MQGetGasPercentage(MQRead(SensorPin) / Ro, GAS_LPG)*100;
  float CO = MQGetGasPercentage(MQRead(SensorPin) / Ro, GAS_CO)*100;
  float Smoke = MQGetGasPercentage(MQRead(SensorPin) / Ro, GAS_SMOKE)*100;


  RetrieveTSChannelData();      // Receive data from Thingspeak

  Serial.println("LPG% = " + (String)LPG);
  Serial.println("CO% = " + (String)CO);
  Serial.println("SMOKE% = " + (String)Smoke);

  if (systemStatus == 1)
  {
    Serial.println("---------------------- System is ON -------------------------");
    float SensorReading = analogRead(SensorPin);
    Serial.println("sensor value = " + (String)SensorReading);
    if (SensorReading > sensorThres)
    {
      if (buzzerStatus == 1)  // Buzzer is ON
      {
        digitalWrite(AlarmPin, HIGH); // make Buzzer Pin Enable
        Serial.println("---------------------- buzzer is ON -------------------------");
      }

      else {
        digitalWrite(AlarmPin, LOW);  // Buzzer Is Off
        Serial.println("---------------------- buzzer Is off -------------------------");
      }

      if (windowStatus == 1) // Window is Open
      {
        digitalWrite(WindowPin, HIGH);     //Make Window Pin Enable
        Serial.println("---------------------- Window is Open Now -------------------------");
      }

      else {
        digitalWrite(WindowPin, LOW);  // Window is Close
        Serial.println("---------------------- Window is Currently Close -------------------------");
      }
    }
    else
    {
      digitalWrite(AlarmPin, LOW);
      Serial.println("---------------------- buzzer off -------------------------");

      if (windowStatus == 1) {
        digitalWrite(WindowPin, HIGH);
      }
      else {
        digitalWrite(WindowPin, LOW);
      }
    }

    PostData(SensorReading, LPG, CO, Smoke);
  }

  else {
    digitalWrite(AlarmPin, LOW);
    Serial.println("=============SYSTEM OFFLINE============");
  }

  Serial.println("==============================================================================================================");
}

void PostData(float SensorReading, float lpg, float co, float smoke) {

  if (client.connect(host, 80)) {
    String postStr = apiKey;
    postStr += "&field1=";
    postStr += String(SensorReading);
    postStr += "&field2=";
    postStr += String(lpg);
    postStr += "&field3=";
    postStr += String(co);
    postStr += "&field4=";
    postStr += String(smoke);
    postStr += "&field5=";
    postStr += String(5);
    postStr += "\n";


    client.print("POST /update HTTP/1.1\n");
    client.print("Host: api.thingspeak.com\n");
    client.print("Connection: close\n");
    client.print("X-THINGSPEAKAPIKEY: " + apiKey + "\n");
    client.print("Content-Type: application/x-www-form-urlencoded\n");
    client.print("Content-Length: ");
    client.print(postStr.length());
    client.print("\n\n\n\n\n\n\n\n");
    client.print(postStr);
  }
  client.stop();
}

void RetrieveTSChannelData() {  // Receive data from Thingspeak
  static char responseBuffer[3 * 1024]; // Buffer for received data
  client = server.available();
  if (!client.connect(host, httpPort)) {
    Serial.println("connection failed");
    return;
  }
  String url = "/channels/" + channelID;
  url += "/feeds.json?results=1";
  client.print(String("GET ") + url + " HTTP/1.1\r\n" + "Host: " + host + "\r\n" + "Connection: close\r\n\r\n");
  while (!skipResponseHeaders());
  while (client.available()) {
    String line = client.readStringUntil('\n');
    if ( line.indexOf('{', 0) >= 0 ) {
      line.toCharArray(responseBuffer, line.length());
      decodeJSON(responseBuffer);
    }
  }
  client.stop();
}

bool skipResponseHeaders() {
  char endOfHeaders[] = "\r\n\r\n";
  client.setTimeout(3000);
  bool ok = client.find(endOfHeaders);
  if (!ok) {
    Serial.println("No response or invalid response!");
  }
  return ok;
}

boolean decodeJSON(char *json) {
  StaticJsonBuffer <3 * 1024> jsonBuffer;
  char *jsonstart = strchr(json, '{');
  if (jsonstart == NULL) {
    Serial.println("JSON data missing");
    return false;
  }
  json = jsonstart;
  JsonObject& root = jsonBuffer.parseObject(json);
  if (!root.success()) {
    Serial.println(F("jsonBuffer.parseObject() failed"));
    return false;
  }

  JsonObject& root_data = root["feeds"][0];
  String System = root_data["field1"];
  String window = root_data["field2"];
  String notify = root_data["field3"];
  systemStatus = System.toInt();
  windowStatus = window.toInt();
  buzzerStatus = notify.toInt();
}


