#include <Servo.h>
#include <HardwareSerial.h>
#include <SPI.h>
#include <MFRC522.h>
#include "BluetoothSerial.h"

//--------------------INICIO RFID--------------------
#define SS_PIN 21
#define RST_PIN 14
MFRC522 rfid(SS_PIN, RST_PIN); // Instance of the class
MFRC522::MIFARE_Key key; 
// Init array that will store new NUID 
byte nuidPICC[4];
//--------------------FIN RFID--------------------


#define TONE_USE_INT
#define TONE_PITCH 440
#include "TonePitch.h"

TaskHandle_t Task1;

#define PIN_PULSADOR_ARRIBA 34
#define PIN_SERVO 25

#define MAX_CANT_SENSORES 4
#define SENSOR_PULSADOR_ARRIBA 0
#define SENSOR_PULSADOR_ABAJO 1
#define SENSOR_PROXIMIDAD 2
#define SENSOR_RFID 3
#define MAX_ESTADOS 5
#define MAX_EVENTOS 8
#define MAX_VERIFICATIONS 4

#define TRIG_PIN 15 // Ultrasonic Sensor's TRIG pin - Pulso para comenzar con medición
#define ECHO_PIN 5  // Ultrasonic Sensor's ECHO pin - Mide el largo del pulso para obtener la distancia
#define LED_ROJO 3
#define LED_VERDE 2
#define PIN_BUZZER 4
#define SS_PIN 21   // Pin SS (Slave Select) del lector RFID
#define RST_PIN 16 // Pin de reinicio del lector RFID
#define UMBRAL_DIFERENCIA_TIMEOUT 3000 //15 segundos
#define UMBRAL_DIFERENCIA_TIMEOUT_2 1000 // 1 segundo
#define TIME_DIFF_BETWEEN_EXEC_CYCLES 60
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
#define TASK_PRIORITY -1
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
  ST_BARRERA_ABIERTA,
  ST_BARRERA_ABIERTA_MANUAL,
  ST_INTENCION_BAJAR
} estado_actual;
String estados_string[] = {"ST_IDLE", "ST_ESPERANDO_RESPUESTA", "ST_BARRERA_ABIERTA", "ST_BARRERA_ABIERTA_MANUAL", "ST_INTENCION_BAJAR"};

// Definición de los eventos
enum eventos
{
  EV_CONTINUAR,
  EV_PULSADOR,
  EV_TIMEOUT,
  EV_LEER_RIFD,
  EV_NO_AUTORIZADO,
  EV_AUTORIZADO,
  EV_LIBRE,
  EV_OCUPADO
} nuevo_evento;
String eventos_string[] = {"EV_CONTINUAR", "EV_PULSADOR", "EV_TIMEOUT", "EV_LEER_RIFD", "EV_NO_AUTORIZADO", "EV_AUTORIZADO", "EV_LIBRE", "EV_OCUPADO"};

// Prototipos de funciones
void start();
void tomar_evento();
void moverServo(int);
bool verificarPulsadorArriba();
bool verificarEntradaRFID();
bool verificarSensorProximidad();
bool verificarBluetooth();
void log(String mensaje);
bool stimeout();
void none();
void pasar_a_idle();
void pasar_a_barrera_abierta();
void pasar_a_barrera_abierta_m();
void pasar_a_esperando_respuesta();
void pasar_a_int_bajar();

int arrayCodigoTarjeta[4] = {0, 0, 0, 0};
bool check_timeout = false;
int current_timeout_target = UMBRAL_DIFERENCIA_TIMEOUT;

int index_verification = 0;
typedef bool (*verif)();
verif verification[MAX_VERIFICATIONS] =
{
  verificarPulsadorArriba,
  verificarSensorProximidad,
  verificarEntradaRFID,
  verificarBluetooth
};

BluetoothSerial SerialBT;


typedef void (*transition)();
transition state_table[MAX_ESTADOS][MAX_EVENTOS] =
    {
        {none, pasar_a_barrera_abierta_m, none, pasar_a_esperando_respuesta, none, none, none,none }, //state ST_IDLE
        {none, none, pasar_a_idle, none, pasar_a_idle, pasar_a_barrera_abierta, none, none }, //state ST_ESPERANDO_RESPUESTA
        {none, pasar_a_idle, pasar_a_int_bajar, none, none, pasar_a_int_bajar, pasar_a_int_bajar, none }, //state ST_BARRERA_ABIERTA
        {none, pasar_a_idle, pasar_a_idle, none, none, none, none, none }, //state ST_BARRERA_ABIERTA_MANUAL
        {none, none, pasar_a_idle, none, none, none, none, pasar_a_barrera_abierta } //state ST_INTENCION_BAJAR
};
// EVENTOS {"EV_CONTINUAR", "EV_PULSADOR", "EV_TIMEOUT", "EV_LEER_RFID", "EV_NO_AUTORIZADO", "EV_AUTORIZADO", "EV_LIBRE", "EV_OCUPADO"};
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
  check_timeout = false;
}

void start_tiempo_desde(int timeout_target)
{
  check_timeout = true;
  current_timeout_target = timeout_target;
  tiempoDesde = millis();
}

void levantar_barrera()
{
  start_tiempo_desde(UMBRAL_DIFERENCIA_TIMEOUT);
  Serial.println("SE PASA A BARRERA ABIERTA");
  moverServo(ANGULO_PULSADO);
  analogWrite(LED_ROJO, COLOR_APAGADO);
  analogWrite(LED_VERDE, COLOR__ENCENDIDO);
}

void pasar_a_barrera_abierta()
{
  levantar_barrera();

  estado_actual = ST_BARRERA_ABIERTA;

    nuevo_evento = EV_CONTINUAR;
  // if(nuevo_evento == EV_PULSADOR){
  //   return;
  // }
}

void pasar_a_barrera_abierta_m()
{
  levantar_barrera();

  estado_actual = ST_BARRERA_ABIERTA_MANUAL;

  if(nuevo_evento == EV_PULSADOR){
    nuevo_evento = EV_CONTINUAR;
    return;
  }
}


void pasar_a_esperando_respuesta()
{
  Serial.println("Esperando autorización para poder entrar...");
  SerialBT.printf("%d %d %d %d \n", arrayCodigoTarjeta[0],arrayCodigoTarjeta[1],arrayCodigoTarjeta[2],arrayCodigoTarjeta[3]);
  estado_actual = ST_ESPERANDO_RESPUESTA;
  start_tiempo_desde(UMBRAL_DIFERENCIA_TIMEOUT);
}

void pasar_a_int_bajar()
{
  start_tiempo_desde(UMBRAL_DIFERENCIA_TIMEOUT_2);
  estado_actual = ST_INTENCION_BAJAR;
  nuevo_evento == EV_CONTINUAR;
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

  //RFID:
  SPI.begin(); // Init SPI bus
  rfid.PCD_Init(); // Init MFRC522 

  for (byte i = 0; i < 6; i++) {
    key.keyByte[i] = 0xFF;
  }

  SerialBT.begin("CGT_VIRTUAL");
  Serial.println(SerialBT.getBtAddressString());

  // Serial.println(F("This code scan the MIFARE Classsic NUID."));
  // Serial.print(F("Using the following key:"));
  // printHex(key.keyByte, MFRC522::MF_KEY_SIZE);
}

void fsm()
{
  tomar_evento();

  if(nuevo_evento >= 0 && nuevo_evento < MAX_EVENTOS && estado_actual >= 0 && estado_actual < MAX_ESTADOS)
  {
    int estado_ant = estado_actual;

    state_table[estado_actual][nuevo_evento]();

    if( nuevo_evento != EV_CONTINUAR && estado_ant != estado_actual)
    {
      //DebugPrintEstado(states_s[estado_actual], events_s[nuevo_evento]);
      Serial.println("ESTADO ACTUAL: " + estados_string[estado_actual]);
      Serial.println("EVENTO: " + eventos_string[nuevo_evento]);
    }
  } else 
  {
    /*HACER: Loguear errores*/
    //DebugPrintEstado(estados_string[ST_ERROR], eventos_string[EV_UNKNOW]);
  }

  //nuevo_evento   = EV_CONTINUAR;
}

void tomar_evento()
{
  /*/long ct = millis();
  long diff = (ct - lct);
  bool timeout = (diff > TIME_DIFF_BETWEEN_EXEC_CYCLES);

  /*if(timeout)
  {*/
    //lct = ct;
    int index = index_verification++ % MAX_VERIFICATIONS;
    if(verification[index]())
    {
      return;
    }

    if(check_timeout)
    {
      if (stimeout(current_timeout_target)) 
      {
        nuevo_evento = EV_TIMEOUT;
        return;
      }
    }
  //}

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
  long ct = millis();
  long diff = (ct - lct);
  bool timeout = (diff > TIME_DIFF_BETWEEN_EXEC_CYCLES);

  if(!timeout)
  {
    return false;
  }
  lct = ct;
  sensores[SENSOR_PROXIMIDAD].valor_actual_analogico = leerSensorDistancia();

  float valor_actual = sensores[SENSOR_PROXIMIDAD].valor_actual_analogico;

  if(valor_actual > UMBRAL_DISTANCIA_CM) //si la distancia es mayor de 50
  {
    nuevo_evento = EV_LIBRE;
    return true;
  }
  else 
  {
    nuevo_evento = EV_OCUPADO;
    return true;
  }

  return false;
}

bool verificarEntradaRFID()
{
  
  if ( ! rfid.PICC_IsNewCardPresent())
    return false;

  // Verify if the NUID has been readed
  if ( ! rfid.PICC_ReadCardSerial())
    return false;

  //Serial.print(F("PICC type: "));
  MFRC522::PICC_Type piccType = rfid.PICC_GetType(rfid.uid.sak);
  //Serial.println(rfid.PICC_GetTypeName(piccType));

  // Check is the PICC of Classic MIFARE type
  if (piccType != MFRC522::PICC_TYPE_MIFARE_MINI &&  
    piccType != MFRC522::PICC_TYPE_MIFARE_1K &&
    piccType != MFRC522::PICC_TYPE_MIFARE_4K) {
    Serial.println(F("Tu tarjeta no es del tipo MIFARE Classic."));
    return false;
  }

  for (byte i = 0; i < rfid.uid.size; i++) {
    arrayCodigoTarjeta[i] = rfid.uid.uidByte[i];
  }

  nuevo_evento = EV_LEER_RIFD;
  return true;
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

bool verificarBluetooth()
{
  if(SerialBT.available())
  {
    switch(SerialBT.read())
    {
      case 'A':
        nuevo_evento = EV_AUTORIZADO;
        break;
      case 'N':
        nuevo_evento = EV_NO_AUTORIZADO;
        playTuneSecondCore(TaskPlayAccessDenied, "Denegado");
        break;
      case 'T':
        nuevo_evento = EV_AUTORIZADO;
        playTuneSecondCore(TaskPlayAccessAllow, "Tarde");
        break;
      case 'P':
        nuevo_evento = EV_PULSADOR;
        break;
      default:
        return false;
    }
    return true;
  }
}