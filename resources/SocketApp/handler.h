#include "WProgram.h"
#include <string.h>


char* setPinMode(char* msg){
  char pin[] = {msg[2],msg[3]};
  char mode = msg[4];
  
  if (mode == 'o'){
    pinMode(atoi(pin), OUTPUT);
    return "OK\n";
  }else if (mode == 'i'){
    pinMode(atoi(pin), INPUT);
    return "OK\n";
  }else
    return "Error.\n";
}

char* setDigitalWrite(char* msg){
  char pin[] = {msg[2],msg[3]};
  char mode = msg[4];
  
  if (mode == 'h'){
    digitalWrite(atoi(pin), HIGH);
    return "OK\n";
  }else if (mode == 'l'){
    digitalWrite(atoi(pin), LOW);
    return "OK\n";
  }else
    return "Error.\n";
}

char* process(char* msg){
  if (msg[0] == 'p' && msg[1] == 'm')
    return setPinMode(msg);
  else if (msg[0] == 'd' && msg[1] == 'w')
    return setDigitalWrite(msg);
  else
    return "Unknown Command.\n";
}
