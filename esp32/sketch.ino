#include <ESP32Servo.h>
#include <HardwareSerial.h>
#include <SPI.h>
// #include <MFRC522.h>

#define TONE_USE_INT
#define TONE_PITCH 440
#include "TonePitch.h"

TaskHandle_t Task1;

#define PIN_PULSADOR_ARRIBA 4
#define PIN_PULSADOR_ABAJO 12
#define PIN_SERVO 18

#define MAX_CANT_SENSORES 4
#define SENSOR_PULSADOR_ARRIBA 0
#define SENSOR_PULSADOR_ABAJO 1
#define SENSOR_PROXIMIDAD 2
#define SENSOR_RFID 3
#define MAX_ESTADOS 3
#define MAX_EVENTOS 8

#define TRIG_PIN 19 // Ultrasonic Sensor's TRIG pin - Pulso para comenzar con medición
#define ECHO_PIN 5  // Ultrasonic Sensor's ECHO pin - Mide el largo del pulso para obtener la distancia
#define LED_ROJO 2
#define LED_VERDE 15
#define PIN_BUZZER 21
#define SS_PIN 5   // Pin SS (Slave Select) del lector RFID
#define RST_PIN 16 // Pin de reinicio del lector RFID
#define UMBRAL_DIFERENCIA_TIMEOUT 5000 //5 segundos
#define UMBRAL_DISTANCIA_CM 50
#define CARACTER_LEER_RFID 'R'
#define CARACTER_AUTORIZADO_A_TIEMPO 'A'
#define CARACTER_AUTORIZADO_TARDE 'T'
#define CARACTER_NO_AUTORIZADO 'N'

#define ANGULO_PULSADO 0
#define ANGULO_NO_PULSADO 90
#define DEBOUNCE_DELAY 100

#define CORE_ZERO 0
#define TASK_STACK_SIZE 1000
#define TASK_PRIORITY 1
#define D_NOTA 4
#define D_MEDIA_NOTA 8

#define COLOR__ENCENDIDO 255
#define COLOR_APAGADO 0
#define MITAD_VELOCIDAD_DEL_SONIDO  0.01723
#define SEGUNDO_A_MILISEGUNDOS  1000
#define SEGUNDO_A_MICROSEGUNDOS  1000000L

// MFRC522 rfid(SS_PIN,RST_PIN);
// MFRC522::MIFARE_KEY key;

// Estructura para los sensores
struct stSensor
{
  int pin;
  int estado;
  long valor_actual_analogico;
  long valor_previo_analogico;
  int valor_actual_digital;
  int valor_previo_digital;
};

// Variables globales
stSensor sensores[MAX_CANT_SENSORES];
Servo Servo1;
float duration_us, distance_cm;
long lct;
long tiempoDesde;
long tiempoDesde2;

long previousDebounceTime = 0; 

// Definición de los estados
enum estados
{
  ST_IDLE,
  ST_ESPERANDO_RESPUESTA,
  ST_BARRERA_ABIERTA
} estado_actual;
String estados_string[] = {"ST_IDLE", "ST_ESPERANDO_RESPUESTA", "ST_BARRERA_ABIERTA"};

// Definición de los eventos
enum eventos
{
  EV_CONTINUAR,
  EV_PULSADOR,
  EV_TIMEOUT,
  EV_LEER_RIFD,
  EV_NO_AUTORIZADO,
  EV_AUTORIZADO,
  EV_DISTANCIA
} nuevo_evento;
String eventos_string[] = {"EV_CONTINUAR", "EV_PULSADOR", "EV_TIMEOUT", "EV_LEER_RIFD", "EV_NO_AUTORIZADO", "EV_AUTORIZADO", "EV_DISTANCIA"};

// Prototipos de funciones
void start();
void tomar_evento();
void moverServo(int);
bool verificarPulsador();
bool verificarRIFD();
bool verificarSensorProximidad();
void log(String mensaje);
bool stimeout();
void none();
void pasar_a_idle();
void pasar_a_barrera_abierta();
void pasar_a_esperando_respuesta();


typedef void (*transition)();
transition state_table[MAX_ESTADOS][MAX_EVENTOS] =
    {
        {none, pasar_a_barrera_abierta, none, pasar_a_esperando_respuesta, none, none, none},//state ST_IDLE
        {none, none, pasar_a_idle, pasar_a_esperando_respuesta, pasar_a_idle, pasar_a_barrera_abierta, none},//state ST_ESPERANDO_RESPUESTA
        {none, pasar_a_idle, pasar_a_idle, none, none, none, pasar_a_idle} //state ST_BARRERA_ABIERTA
};
// EVENTOS {"EV_CONTINUAR", "EV_PULSADOR", "EV_TIMEOUT", "EV_LEER_RFID", "EV_NO_AUTORIZADO", "EV_AUTORIZADO", "EV_DISTANCIA"};
/**********************************************************************************************/

void none() //aca verifica el timeout de 5 segundos, 
{

}

void pasar_a_idle()
{
  Serial.println("Pasar a Idle");
  // digitalWrite(PIN_PULSADOR, LOW);
  moverServo(ANGULO_NO_PULSADO);
  analogWrite(LED_ROJO, COLOR__ENCENDIDO);
  analogWrite(LED_VERDE, COLOR_APAGADO);
  // digitalWrite(BUZZER, LOW);
  estado_actual = ST_IDLE;
}

void pasar_a_barrera_abierta()
{
  tiempoDesde = millis();
    Serial.println("SE PASA A BARRERA ABIERTA");
    moverServo(ANGULO_PULSADO);
    analogWrite(LED_ROJO, COLOR_APAGADO);
    analogWrite(LED_VERDE, COLOR__ENCENDIDO);

    estado_actual = ST_BARRERA_ABIERTA;

    if(nuevo_evento == EV_PULSADOR){
      nuevo_evento = EV_CONTINUAR;
      return;
    }
}


void pasar_a_esperando_respuesta()
{
  Serial.println("Esperando autorización para poder entrar...");
  estado_actual = ST_ESPERANDO_RESPUESTA;   
  tiempoDesde = millis();
}

void setup()
{
  start();
}

void loop()
{
  fsm();
}

void start()
{
  Serial.begin(9600);
  // Inicializo el estado del embebido
  estado_actual = ST_IDLE;
  Servo1.attach(PIN_SERVO);
  // SPI.begin();      // Inicializa la comunicación SPI
  // rfid_PCD.init();      // Inicializa el lector RFID
  // Asigno los pines a los sensores correspondientes
  pinMode(PIN_PULSADOR_ARRIBA, INPUT);
  pinMode(PIN_PULSADOR_ABAJO, INPUT);

  // Se configura el pin TRIG como salida para el sensor ultrasónico.
  pinMode(TRIG_PIN, OUTPUT);
  // Se configura el pin ECHO como entrada para el sensor ultrasónico.
  pinMode(ECHO_PIN, INPUT);
  
  pinMode(LED_ROJO, OUTPUT);
  pinMode(LED_VERDE, OUTPUT);

  pinMode(PIN_BUZZER, OUTPUT);

  analogWrite(LED_ROJO, COLOR__ENCENDIDO);

  lct = millis(); // Guarda el tiempo actual al inicio
  sensores[SENSOR_PULSADOR_ARRIBA].valor_actual_digital = LOW;
  sensores[SENSOR_PULSADOR_ARRIBA].valor_previo_digital = LOW;

  Serial.print("LCT: ");
  Serial.println(lct);
  playTuneSecondCore(TaskPlayBoot, "boot");
}

void fsm()
{
  tomar_evento();

  if(nuevo_evento >= 0 && nuevo_evento < MAX_EVENTOS && estado_actual >= 0 && estado_actual < MAX_ESTADOS)
  {
    if( nuevo_evento != EV_CONTINUAR )
    {
      //DebugPrintEstado(states_s[estado_actual], events_s[nuevo_evento]);
      Serial.println("ESTADO ACTUAL: " + estados_string[estado_actual]);
      Serial.println("EVENTO: " + eventos_string[nuevo_evento]);
    }

    state_table[estado_actual][nuevo_evento]();
  } else 
  {
    /*HACER: Loguear errores*/
    //DebugPrintEstado(estados_string[ST_ERROR], eventos_string[EV_UNKNOW]);
  }

  //nuevo_evento   = EV_CONTINUAR;
}

void tomar_evento()
{
  if(verificarPulsadorArriba()
    || verificarSensorProximidad() || verificarEntradaTeclado())
  {
    return;
  }

  if((estado_actual == ST_BARRERA_ABIERTA || estado_actual == ST_ESPERANDO_RESPUESTA) ){
    if (stimeout(UMBRAL_DIFERENCIA_TIMEOUT)) 
    {
      Serial.println("Han pasado 5 segundos y NO fue autorizado, se retorna al estado IDLE");
      nuevo_evento = EV_TIMEOUT;
      return;
    }
  }

  nuevo_evento = EV_CONTINUAR;
}

void moverServo(int angulo)
{
  Servo1.write(angulo);
}

bool verificarPulsadorArriba()
{
  sensores[SENSOR_PULSADOR_ARRIBA].valor_actual_digital = digitalRead(PIN_PULSADOR_ARRIBA);
  int valor_actual_aux = sensores[SENSOR_PULSADOR_ARRIBA].valor_actual_digital;
  int valor_previo_aux = sensores[SENSOR_PULSADOR_ARRIBA].valor_previo_digital;
 
  // Si hay un cambio en el estado del pulsador
  if ((millis() - previousDebounceTime) > DEBOUNCE_DELAY 
    && valor_actual_aux != valor_previo_aux)
  {
    previousDebounceTime = millis(); 
    // ENCENDER PULSADOR
    if(valor_actual_aux == HIGH){
      Serial.println("PULSADOR ARRIBA ENCENDIDO!");
      nuevo_evento = EV_PULSADOR;
      sensores[SENSOR_PULSADOR_ARRIBA].valor_previo_digital = valor_actual_aux;
      return true;
    }
  }

  sensores[SENSOR_PULSADOR_ARRIBA].valor_previo_digital = valor_actual_aux;
  
  return false;
}

float leerSensorDistancia()
{
  //Desactivo el trigger
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  
  //Activo el Trigger por 10 microsegundos
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  
   //Desactivo el trigger
  digitalWrite(TRIG_PIN, LOW);
    
  //Leo el pin de Echo, y lo multiplico para retornar la distancia
  return pulseIn(ECHO_PIN, HIGH) * MITAD_VELOCIDAD_DEL_SONIDO;
}

bool verificarSensorProximidad()
{
  sensores[SENSOR_PROXIMIDAD].valor_actual_analogico = leerSensorDistancia();

  float valor_actual = sensores[SENSOR_PROXIMIDAD].valor_actual_analogico;

  if(valor_actual > UMBRAL_DISTANCIA_CM) //si la distancia es mayor de 50
  {
    nuevo_evento = EV_DISTANCIA;
    return true;
  }

  return false;
}

bool verificarRIFD()
{
  if (Serial.available() > 0) {
    // Lee la cadena completa del monitor serial
    char input = Serial.read();
    
    // Imprime la cadena leída en el monitor serial
    Serial.print("Caracter recibido: ");
    Serial.println(input);

    if (input == CARACTER_LEER_RFID) {
      nuevo_evento = EV_LEER_RIFD;
      Serial.println("Se está verificando su RFID...");
      return true;
    } else {
      Serial.println("ERROR. RFID leido incorrectamente");
      return false;
    }
  }
  return false;
}

bool verificarEntradaTeclado()
{
  if (Serial.available() > 0) {
    // Lee la cadena completa del monitor serial
    char input = Serial.read();
    
    // Imprime la cadena leída en el monitor serial
    Serial.print("Caracter recibido: ");
    Serial.println(input);

    if (input == CARACTER_LEER_RFID) {
      nuevo_evento = EV_LEER_RIFD;
      Serial.println("Se está verificando su RFID...");
      return true;
    } else if (input == CARACTER_AUTORIZADO_A_TIEMPO) {
      Serial.println("Llegó en horario... AUTORIZADO");
      nuevo_evento = EV_AUTORIZADO;
      return true;
    } else if (input == CARACTER_AUTORIZADO_TARDE ) {
      Serial.println("NO llegó en horario... AUTORIZADO");
      nuevo_evento = EV_AUTORIZADO;
      playTuneSecondCore(TaskPlayAccessAllow, "Late");
      return true;
    } else if (input == CARACTER_NO_AUTORIZADO) {
      Serial.println("NO AUTORIZADO");
      nuevo_evento = EV_NO_AUTORIZADO;
      playTuneSecondCore(TaskPlayAccessDenied, "Denied");      
      return true;
    }
    return false;
  }
  return false;
}


bool stimeout(unsigned long intervalo) {
  /****************************/ 
    //Serial.print("tiempo desde: ");
    //Serial.println(tiempoDesde);

    long tiempoHastaAhora = millis(); //desde que se inicio el programa
    //Serial.print("tiempo hasta ahora: ");
    //Serial.println(tiempoHastaAhora);

    long tiempoQuePaso = tiempoHastaAhora - tiempoDesde;
    //Serial.print("tiempo que paso: ");
    //Serial.println(tiempoQuePaso);

    if(tiempoQuePaso > intervalo)
    {
      //Serial.println("Retorna TRUE");
      return true;
    }else{
      //Serial.println("Retorna FALSE");
      return false;
    }
  /****************************/ 
  //return (millis() - lct >= intervalo);

}

void customTone(byte pin, uint16_t frequency, uint16_t duration)
{
  unsigned long startTime=millis();
  unsigned long halfPeriod= SEGUNDO_A_MICROSEGUNDOS/frequency/2;
  pinMode(pin,OUTPUT);
  while (millis()-startTime< duration)
  {
    digitalWrite(pin,HIGH);
    delayMicroseconds(halfPeriod);
    digitalWrite(pin,LOW);
    delayMicroseconds(halfPeriod);
  }
  pinMode(pin,INPUT);
}

void play(int *melody, int *durations, int size)
{
  for (int note = 0; note < size; note++)
  {
    // Para calcular la duración de la nota, se divide un segundo por la duración de la nota
    int duration = SEGUNDO_A_MILISEGUNDOS / durations[note];
    customTone(PIN_BUZZER, melody[note], duration);
  }
}

void playTuneSecondCore(TaskFunction_t pvTaskCode, const char *constpcName)
{
  if(!Task1 || eTaskGetState(Task1) == eDeleted)
  {
    xTaskCreatePinnedToCore(
      pvTaskCode,     // Función de la tarea
      constpcName,    // Nombre de la tarea. 
      TASK_STACK_SIZE,// Tamaño del Stack de la tarea 
      NULL,           // Parametros de la tarea
      TASK_PRIORITY,  // Prioridad de la tarea
      &Task1,         // Handle de la tarea, para seguirla al ser creada
      CORE_ZERO
    );
  }
}

void TaskPlayAccessAllow(void * pvParameters)
{
  int melody[] = {NOTE_E4, NOTE_F4, NOTE_G4};
  int durations[] = {D_MEDIA_NOTA, D_MEDIA_NOTA, D_MEDIA_NOTA};
  play(melody, durations, sizeof(durations) / sizeof(int));
  vTaskDelete(NULL);
}

void TaskPlayAccessDenied(void * pvParameters)
{
  int melody[] = {NOTE_G5, NOTE_G5, NOTE_G5};
  int durations[] = {D_MEDIA_NOTA, D_MEDIA_NOTA, D_MEDIA_NOTA};
  play(melody, durations, sizeof(durations) / sizeof(int));
  vTaskDelete(NULL);
}

void TaskPlayBoot(void * pvParameters)
{
  int melody[] = {NOTE_G5, NOTE_F5, NOTE_G5};
  int durations[] = {D_NOTA, D_MEDIA_NOTA, D_NOTA};
  play(melody, durations, sizeof(durations) / sizeof(int));
  vTaskDelete(NULL);
}
