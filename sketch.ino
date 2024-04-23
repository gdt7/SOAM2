
#include <ESP32Servo.h>
#include <HardwareSerial.h>
#include <SPI.h>
//#include <MFRC522.h>
#include <LiquidCrystal.h>

LiquidCrystal lcd(22,23,15,34,35,33);
#define PIN_PULSADOR                                4
#define PIN_SERVO                                   18

#define MAX_CANT_SENSORES                           3
#define SENSOR_PULSADOR                             0
#define SENSOR_PROXIMIDAD                           1
#define SENSOR_RFID                                 2 
#define MAX_ESTADOS                                 3
#define MAX_EVENTOS                                 6

#define TRIG_PIN 19 // ESP32 pin GIOP23 connected to Ultrasonic Sensor's TRIG pin - Pulse to start the measurement
#define ECHO_PIN 5 // ESP32 pin GIOP22 connected to Ultrasonic Sensor's ECHO pin - Measure the high pulse length to get the distance
#define LED 2
#define BUZZER 21
#define SS_PIN  5   // Pin SS (Slave Select) del lector RFID
#define RST_PIN 16  // Pin de reinicio del lector RFID

#define ANGULO_PULSADO                              0
#define ANGULO_NO_PULSADO                           90

//MFRC522 rfid(SS_PIN,RST_PIN);
//MFRC522::MIFARE_KEY key;

// Estructura para los sensores
struct stSensor
{
  int  pin;
  int  estado;
  long valor_actual;
  long valor_previo;
};

// Variables globales
stSensor sensores[MAX_CANT_SENSORES];
Servo Servo1;
float duration_us, distance_cm;

// Definición de los estados
enum estados          { ST_IDLE        , 
                        ST_ESPERANDO_RESPUESTA   , 
                        ST_BARRERA_ABIERTA } estado_actual;
String estados_string [] = {"ST_IDLE"      , "ST_ESPERANDO_RESPUESTA" , "ST_BARRERA_ABIERTA"};

// Definición de los eventos
enum eventos          { EV_PULSADOR,   
                        EV_TIMEOUT,   
                        EV_LEER_RIFD,   
                        EV_NO_AUTORIZADO,   
                        EV_AUTORIZADO,   
                        EV_DISTANCIA   } nuevo_evento;
String eventos_string [] = {"EV_PULSADOR", "EV_TIMEOUT", "EV_LEER_RIFD", "EV_NO_AUTORIZADO", "EV_AUTORIZADO", "EV_DISTANCIA"};


// Prototipos de funciones
void start();
void tomar_evento();
void moverServo(int);
void verificarPulsador(); 
void log(String mensaje);



void none()
{
  // No se realiza ninguna acción
}

void pasar_a_idle()
{   
    //digitalWrite(PIN_PULSADOR, LOW);
    moverServo(ANGULO_NO_PULSADO);
    digitalWrite(LED, LOW);
    digitalWrite(BUZZER, LOW);
    estado_actual = ST_IDLE;
}

void pasar_a_barrera_abierta()
{
    moverServo(ANGULO_PULSADO); 
    //digitalWrite(PIN_PULSADOR, HIGH);
    digitalWrite(LED, HIGH);
    digitalWrite(BUZZER, HIGH);
    estado_actual = ST_BARRERA_ABIERTA;
}

void pasar_a_esperando_respuesta()
{
    estado_actual = ST_ESPERANDO_RESPUESTA;
}

typedef void (*transition)();
transition state_table[MAX_ESTADOS][MAX_EVENTOS] =
{
      {pasar_a_barrera_abierta,   none,                pasar_a_esperando_respuesta,    none,           none,                       pasar_a_barrera_abierta},   // state ST_IDLE
      {none,                      pasar_a_idle,        none,                           pasar_a_idle,   pasar_a_barrera_abierta,    none        } ,  // state ST_ESPERANDO_RESPUESTA
      {pasar_a_idle,              pasar_a_idle,        none,                           none,           none,                       pasar_a_idle}    // state ST_BARRERA_ABIERTA
};



/**********************************************************************************************/

void setup() {
  start();
}


void loop() {
    fsm();

      // Verifica si hay una tarjeta RFID presente
  /*if (rfid.isCard()) {
    // Lee el número de serie de la tarjeta RFID
    if (rfid.readCardSerial()) {
      Serial.print("Tarjeta detectada: ");
      // Imprime el número de serie de la tarjeta RFID
      Serial.print(rfid.serNum[0], DEC);
      Serial.print(" ");
      Serial.print(rfid.serNum[1], DEC);
      Serial.print(" ");
      Serial.print(rfid.serNum[2], DEC);
      Serial.print(" ");
      Serial.print(rfid.serNum[3], DEC);
      Serial.print(" ");
      Serial.println(rfid.serNum[4], DEC);
    }
  }
  delay(1000); // Espera un segundo antes de la próxima lectura
  */
}

void start(){
  Serial.begin(9600);
  //Inicializo el estado del embebido
  estado_actual = ST_IDLE;
  Servo1.attach(PIN_SERVO);
  //SPI.begin();      // Inicializa la comunicación SPI
  //rfid_PCD.init();      // Inicializa el lector RFID
  //Asigno los pines a los sensores correspondientes
  pinMode(PIN_PULSADOR, INPUT);
  // Se configura el pin TRIG como salida para el sensor ultrasónico.
  pinMode(TRIG_PIN, OUTPUT);
  // Se configura el pin ECHO como entrada para el sensor ultrasónico.
  pinMode(ECHO_PIN, INPUT);
  pinMode(LED, OUTPUT);{}
  pinMode(BUZZER, OUTPUT);
   lcd.begin(16, 2);
}

void fsm() {

    tomar_evento();
}

void tomar_evento(){

   // Se genera un pulso de 10 microsegundos en el pin TRIG del sensor ultrasónico para iniciar una medición.
    digitalWrite(TRIG_PIN, HIGH);
    delayMicroseconds(10);
    digitalWrite(TRIG_PIN, LOW);

    // Se mide la duración del pulso de retorno en el pin ECHO utilizando la función pulseIn().
    duration_us = pulseIn(ECHO_PIN, HIGH);
    
    // Calcula la distancia
    distance_cm = 0.017 * duration_us;
    
    // Se actualiza el evento según la distancia detectada
    if (distance_cm < 30) {
        nuevo_evento = EV_DISTANCIA;
        //nuevo_evento = 5;
    } else {
        nuevo_evento = EV_TIMEOUT;
    }

    if (estado_actual >= 0 && estado_actual < MAX_ESTADOS && nuevo_evento >= 0 && nuevo_evento < MAX_EVENTOS) {
        state_table[estado_actual][nuevo_evento]();
    }
}


void moverServo(int angulo) {
  Servo1.write(angulo); 
  delay(15); 
}

void verificarPulsador(){
 if (digitalRead(PIN_PULSADOR) == HIGH) {
   digitalWrite(LED, HIGH);
    moverServo(ANGULO_PULSADO); 
   // distance_cm = 10;
  } 
  //de lo contrario apagamos el led
  else {
    digitalWrite(LED, LOW);
    moverServo(ANGULO_NO_PULSADO);
    //distance_cm = 250;
  }

}
