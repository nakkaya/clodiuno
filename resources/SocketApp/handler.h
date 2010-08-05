#include <WProgram.h>

int parsePin(char* msg){
  char pin[] = {msg[2],msg[3]};
  return atoi(pin);
}

int cmdCmpr(char* msg, char* expected){
  if (msg[0] == expected[0] && msg[1] == expected[1])
    return true;
  else
    return false;
}

char* setPinMode(char* msg){
  int pin = parsePin(msg);
  char mode = msg[4];
  
  if (mode == 'o'){
    digitalWrite(pin, LOW); // disable PWM
    pinMode(pin, OUTPUT);
    return "OK\n";
  }else if (mode == 'i'){
    pinMode(pin, INPUT);
    return "OK\n";
  }else if (mode == 'a'){
    digitalWrite(pin + 14, LOW); // disable internal pull-ups
    pinMode(pin + 14, INPUT);
    return "OK\n";
  }else if (mode == 'p'){
    pinMode(pin, OUTPUT);
    return "OK\n";
  }else
    return "Error\n";
}

char* setDigitalWrite(char* msg){
  int pin = parsePin(msg);
  char mode = msg[4];
  
  if (mode == 'h'){
    digitalWrite(pin, HIGH);
    return "OK\n";
  }else if (mode == 'l'){
    digitalWrite(pin, LOW);
    return "OK\n";
  }else
    return "Error\n";
}

char* setAnalogWrite(char* msg){
  int pin = parsePin(msg);
  char val[] = {msg[4], msg[5], msg[6]};
  
  analogWrite(pin, atoi(val));
  return "OK\n";
}

char* getDigitalRead(char* msg){
  int pin = parsePin(msg);

  char buff [20];
  itoa(digitalRead(pin),buff,10);

  strncat(buff,"\n",1);
  return buff;
}

char* getAnalogRead(char* msg){
  int pin = parsePin(msg);

  char buff [20];
  itoa(analogRead(pin),buff,10);

  strncat(buff,"\n",1);
  return buff;
}

char* process(char* msg){
  if (cmdCmpr(msg,"pm"))
    return setPinMode(msg);
  else if (cmdCmpr(msg,"dw"))
    return setDigitalWrite(msg);
  else if (cmdCmpr(msg,"aw"))
    return setAnalogWrite(msg);
  else if (cmdCmpr(msg,"dr"))
    return getDigitalRead(msg);
  else if (cmdCmpr(msg,"ar"))
    return getAnalogRead(msg);
  else
    return "Unknown Command.\n";
}
