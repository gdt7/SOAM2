#include <ESP32Servo.h>

#define PIN_PULSADOR                                4
#define PIN_SERVO                                   18

#define MAX_CANT_SENSORES                           3
#define SENSOR_PULSADOR                             0
#define SENSOR_PROXIMIDAD                           1
#define SENSOR_RFID                                 2 
#define MAX_ESTADOS                                 3
#define MAX_EVENTOS                                 6

#define ANGULO_PULSADO                              180
#define ANGULO_NO_PULSADO                           90

struct stSensor
{
  int  pin;
  int  estado;
  long valor_actual;
  long valor_previo;
};
stSensor sensores[MAX_CANT_SENSORES];
Servo Servo1;

enum estados          { ST_IDLE        , ST_ESPERANDO_RESPUESTA   , ST_BARRERA_ABIERTA } estado_actual;
String estados_string [] = {"ST_IDLE"      , "ST_ESPERANDO_RESPUESTA" , "ST_BARRERA_ABIERTA"};

enum eventos          { EV_PULSADOR,   EV_TIMEOUT,   EV_LEER_RIFD,   EV_NO_AUTORIZADO,   EV_AUTORIZADO,   EV_DISTANCIA   } nuevo_evento;
String eventos_string [] = {"EV_PULSADOR", "EV_TIMEOUT", "EV_LEER_RIFD", "EV_NO_AUTORIZADO", "EV_AUTORIZADO", "EV_DISTANCIA"};



typedef void (*transition)();

transition state_table[MAX_ESTADOS][MAX_EVENTOS] =
{
      {pasar_a_barrera_abierta,   none,                pasar_a_esperando_respuesta,    none,           none,                       none        },   // state ST_IDLE
      {none,                      pasar_a_idle,        none,                           pasar_a_idle,   pasar_a_barrera_abierta,    none        } ,  // state ST_ESPERANDO_RESPUESTA
      {pasar_a_idle,              pasar_a_idle,        none,                           none,           none,                       pasar_a_idle}    // state ST_BARRERA_ABIERTA
};

void none()
{
  // No se realiza ninguna acción
}

void pasar_a_idle()
{

}

void pasar_a_barrera_abierta()
{

}

void pasar_a_esperando_respuesta()
{

}



void setup() {
  Serial.begin(9600);

  Servo1.attach(PIN_SERVO);

  pinMode(PIN_PULSADOR, INPUT);
}


void loop() {
  if (digitalRead(PIN_PULSADOR) == LOW) {
    moverServo(ANGULO_PULSADO); 
  } 
  //de lo contrario apagamos el led
  else {
    moverServo(ANGULO_NO_PULSADO);
  }
}


void moverServo(int angulo) {
  Servo1.write(angulo); 
  delay(15); 
}

