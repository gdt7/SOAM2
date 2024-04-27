#include <ESP32Servo.h>
#include <HardwareSerial.h>
#include <SPI.h>
// #include <MFRC522.h>
#include <LiquidCrystal.h>

LiquidCrystal lcd(22, 23, 15, 34, 35, 33);
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

#define TRIG_PIN 19 // ESP32 pin GIOP23 connected to Ultrasonic Sensor's TRIG pin - Pulse to start the measurement
#define ECHO_PIN 5  // ESP32 pin GIOP22 connected to Ultrasonic Sensor's ECHO pin - Measure the high pulse length to get the distance
#define LED 2
#define BUZZER 21
#define SS_PIN 5   // Pin SS (Slave Select) del lector RFID
#define RST_PIN 16 // Pin de reinicio del lector RFID
#define UMBRAL_DIFERENCIA_TIMEOUT 5000 //5 segundos
#define UMBRAL_DISTANCIA_CM 50


#define ANGULO_PULSADO 0
#define ANGULO_NO_PULSADO 90

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
bool timeout;
long lct;
long tiempoDesde;
long tiempoDesde2;
int ban = 0;
int ban2 = 0;
int val = 0;
int loopCount = 0;

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
  EV_PULSADOR_ARRIBA,
  EV_PULSADOR_ABAJO,
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

void none()
{
  // No se realiza ninguna acción
}

void pasar_a_idle()
{
  Serial.println("Pasar a Idle");
  // digitalWrite(PIN_PULSADOR, LOW);
  moverServo(ANGULO_NO_PULSADO);
  digitalWrite(LED, LOW);
  digitalWrite(BUZZER, LOW);
  estado_actual = ST_IDLE;
}

void pasar_a_barrera_abierta()
{
    Serial.println("SE PASA A BARRERA ABIERTA");
    moverServo(ANGULO_PULSADO);
    digitalWrite(LED, HIGH);
    digitalWrite(BUZZER, HIGH);
    estado_actual = ST_BARRERA_ABIERTA;

    if(nuevo_evento == EV_PULSADOR_ARRIBA){
      nuevo_evento = EV_CONTINUAR;
      return;
    }

    //Serial.println("Barrera abierta y distancia!");

    
    /*
    if(ban2==0){
      Serial.println("Primera vez que calcula tiempoDesde en barrera abierta");
      tiempoDesde2 = millis();
    }

    if (stimeout(UMBRAL_DIFERENCIA_TIMEOUT)) {
      ban2=0;
      Serial.println("Han pasado 5 segundos! Se cierra la barrera en caso de no detectar nada!");
      nuevo_evento = EV_TIMEOUT;
      pasar_a_idle();
    }
      ban2=1;*/
    
}


void pasar_a_esperando_respuesta()
{
  Serial.println("Esperando autorización para poder entrar...");
  estado_actual = ST_ESPERANDO_RESPUESTA;
  
  /*estado_actual = ST_ESPERANDO_RESPUESTA;

    if(nuevo_evento == EV_PULSADOR){
      nuevo_evento = EV_CONTINUAR;
      return;
    }

    //Serial.print("BAN: ");
    //Serial.println(ban);
  if(ban==0){
    //Serial.println("Primera vez que calcula tiempoDesde");
    tiempoDesde = millis();
  }

  if (stimeout(UMBRAL_DIFERENCIA_TIMEOUT)) {//|| verificarRIFD()) {
    ban=0;
    Serial.println("Han pasado 5 segundos y NO fue autorizado, se retorna al estado IDLE");
    nuevo_evento = EV_TIMEOUT;
    pasar_a_idle();
  }else{

      if(verificarRIFD()){
        Serial.println("Camion autorizado!");
        pasar_a_barrera_abierta();
        return;
      }

    ban=1;
  }*/

    
}

typedef void (*transition)();
transition state_table[MAX_ESTADOS][MAX_EVENTOS] =
    {
        {none, pasar_a_barrera_abierta, none, none, pasar_a_esperando_respuesta, none, none, none},//state ST_IDLE
        {none, none, none, pasar_a_idle, pasar_a_esperando_respuesta, pasar_a_idle, pasar_a_barrera_abierta, none},//state ST_ESPERANDO_RESPUESTA
        {none, none, pasar_a_idle, pasar_a_idle, none, none, none, pasar_a_idle} //state ST_BARRERA_ABIERTA
};
// EVENTOS {"EV_PULSADOR", "EV_TIMEOUT", "EV_LEER_RIFD", "EV_NO_AUTORIZADO", "EV_AUTORIZADO", "EV_DISTANCIA", "EV_CONTINUAR"};
/**********************************************************************************************/

void setup()
{
  start();
}

void loop()
{
  fsm();
  loopCount += 1;
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
  
  pinMode(LED, OUTPUT);
  {
  }
  pinMode(BUZZER, OUTPUT);
  //lcd.begin(16, 2);
  timeout = false;
  lct = millis(); // Guarda el tiempo actual al inicio
  sensores[SENSOR_PULSADOR_ARRIBA].valor_actual_digital = LOW;
  sensores[SENSOR_PULSADOR_ARRIBA].valor_previo_digital = LOW;

  Serial.print("LCT: ");
  Serial.println(lct);
  //Serial.println("EVENTO EN START: " + eventos_string[nuevo_evento]);
}

void fsm()
{
  tomar_evento();
  //Serial.println("ESTADO ACTUAL: " + estados_string[estado_actual]);
  //Serial.println("EVENTO: " + eventos_string[nuevo_evento]);

  if(nuevo_evento >= 0 && nuevo_evento < MAX_EVENTOS && estado_actual >= 0 && estado_actual < MAX_ESTADOS)
  {
    /*if( nuevo_evento != EV_CONTINUAR )
    {
      //DebugPrintEstado(states_s[estado_actual], events_s[nuevo_evento]);
    }*/
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
  /*long ct = millis();
  int diferencia = (ct - lct); 
  Serial.print("CT: ");
  Serial.println(ct);

  Serial.print("DIFERENCIA: ");
  Serial.println(diferencia);

  timeout = (diferencia > UMBRAL_DIFERENCIA_TIMEOUT) ? (true) : (false);
  verificarSensorProximidad(); //ver
  if (timeout)
  {
    timeout = false;
    lct = ct;
  */
    if(verificarPulsadorAbajo() || verificarPulsadorArriba() ||  verificarSensorProximidad() || verificarRIFD())
    {
      return;
    }
  //}

  nuevo_evento = EV_CONTINUAR;
}

void moverServo(int angulo)
{
  Servo1.write(angulo);
  delay(15);
}

bool verificarPulsadorArriba()
{
  sensores[SENSOR_PULSADOR_ARRIBA].valor_actual_digital = digitalRead(PIN_PULSADOR_ARRIBA);
  int valor_actual_aux = sensores[SENSOR_PULSADOR_ARRIBA].valor_actual_digital;
  int valor_previo_aux = sensores[SENSOR_PULSADOR_ARRIBA].valor_previo_digital;
 
  // Si hay un cambio en el estado del pulsador
  if (valor_actual_aux != valor_previo_aux)
  {
    // ENCENDER PULSADOR
    if(valor_actual_aux == HIGH){
      Serial.println("PULSADOR ARRIBA ENCENDIDO!");
      nuevo_evento = EV_PULSADOR_ARRIBA;
      sensores[SENSOR_PULSADOR_ARRIBA].valor_previo_digital = valor_actual_aux;
      return true;
    }
  }

  sensores[SENSOR_PULSADOR_ARRIBA].valor_previo_digital = valor_actual_aux;
  
  // else if (valor_actual_aux == HIGH && valor_previo_aux == HIGH)  
  // {
  //   //APAGAR PULSADOR
  //   Serial.println("PULSADOR ARRIBA APAGADO!");
  //   nuevo_evento = EV_PULSADOR_ARRIBA;
  //   sensores[SENSOR_PULSADOR_ARRIBA].valor_previo_digital = LOW;
  //   return true;
  // }
  
  return false;
}

bool verificarPulsadorAbajo()
{
  sensores[SENSOR_PULSADOR_ABAJO].valor_actual_digital = digitalRead(PIN_PULSADOR_ABAJO);
  int valor_actual_aux = sensores[SENSOR_PULSADOR_ABAJO].valor_actual_digital;
  int valor_previo_aux = sensores[SENSOR_PULSADOR_ABAJO].valor_previo_digital;
 
  // Si hay un cambio en el estado del pulsador
  if (valor_actual_aux != valor_previo_aux)
  {
    // ENCENDER PULSADOR
    if(valor_actual_aux == HIGH){
      Serial.println("PULSADOR ABAJO ENCENDIDO!");
      nuevo_evento = EV_PULSADOR_ABAJO;
      sensores[SENSOR_PULSADOR_ABAJO].valor_previo_digital = valor_actual_aux;
      return true;
    }
  }

  sensores[SENSOR_PULSADOR_ABAJO].valor_previo_digital = valor_actual_aux;

  // else if (valor_actual_aux == HIGH && valor_previo_aux == HIGH)  
  // {
  //   //APAGAR PULSADOR
  //   Serial.println("PULSADOR ARRIBA APAGADO!");
  //   nuevo_evento = EV_PULSADOR_ABAJO;
  //   sensores[SENSOR_PULSADOR_ABAJO].valor_previo_digital = LOW;
  //   return true;
  // }
  
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
  return pulseIn(ECHO_PIN, HIGH) * 0.01723;
}

bool verificarSensorProximidad()
{
  sensores[SENSOR_PROXIMIDAD].valor_actual_analogico = leerSensorDistancia();

   //Serial.print("DISTANCIA: ");
   //Serial.println(leerSensorDistancia());

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
    Serial.print("Cadena recibida: ");
    Serial.println(input);

    if (input == 'R') {
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


bool stimeout(unsigned long intervalo) {
  /****************************/ 
    Serial.print("tiempo desde: ");
    Serial.println(tiempoDesde);

    long tiempoHastaAhora = millis(); //desde que se inicio el programa
    Serial.print("tiempo hasta ahora: ");
    Serial.println(tiempoHastaAhora);

    long tiempoQuePaso = tiempoHastaAhora - tiempoDesde;
    Serial.print("tiempo que paso: ");
    Serial.println(tiempoQuePaso);

    if(tiempoQuePaso > intervalo){
      Serial.println("Retorna TRUE");
        return true;
    }else{
      Serial.println("Retorna FALSE");
        return false;
    }
  /****************************/ 
  //return (millis() - lct >= intervalo);

}