#include <Servo.h>
#include <WiShield.h>
#include "handler.h"

#define WIRELESS_MODE_INFRA	1
#define WIRELESS_MODE_ADHOC	2

// Wireless configuration parameters ----------------------------------------
// IP address of WiShield
unsigned char local_ip[] = {10,0,2,200};
// router or gateway IP address
unsigned char gateway_ip[] = {10,0,2,1};
// subnet mask for the local network
unsigned char subnet_mask[] = {255,255,255,0};
// max 32 bytes
const prog_char ssid[] PROGMEM = {"adhoc"};


// 0 - open; 1 - WEP; 2 - WPA; 3 - WPA2
unsigned char security_type = 0;

// WPA/WPA2 passphrase
// max 64 characters
const prog_char security_passphrase[] PROGMEM = {"12345678"};

// WEP 128-bit keys
// sample HEX keys
prog_uchar wep_keys[] PROGMEM = {	
  0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 
  0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d,	// Key 0
  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
  0x00, 0x00, 0x00, 0x00, 0x00,	0x00,	// Key 1
  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
  0x00, 0x00, 0x00, 0x00, 0x00,	0x00,	// Key 2
  0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
  0x00, 0x00, 0x00, 0x00, 0x00, 0x00	// Key 3
};

// setup the wireless mode
// infrastructure - connect to AP
// adhoc - connect to another WiFi device
unsigned char wireless_mode = WIRELESS_MODE_INFRA;

unsigned char ssid_len;
unsigned char security_passphrase_len;
//---------------------------------------------------------------------------

void setup(){
  WiFi.init();
}

void loop(){
  WiFi.run();
}

extern "C" {

#include "uip.h"
#include <string.h>
#include "uipopt.h"
#include "psock.h"

  static int handle_connection(struct socket_app_state *s);

  void socket_app_init(void){
    /* We start to listen for connections on TCP port 1000. */
    uip_listen(HTONS(1000));
  }

  void socket_app_appcall(void){
    struct socket_app_state *s = &(uip_conn->appstate);

    if(uip_connected()) {
      PSOCK_INIT(&s->p, 
		 (unsigned char*)s->inputbuffer, 
		 sizeof(s->inputbuffer));
    }

    handle_connection(s);
  }

#define PSOCK_SEND_STR(psock, str)					\
  PT_WAIT_THREAD(&((psock)->pt), \
		 psock_send(psock, (unsigned char*)str, strlen(str)))

  static int handle_connection(struct socket_app_state *s){

    PSOCK_BEGIN(&s->p);
    PSOCK_SEND_STR(&s->p,"Connected.\n");

    for(;;){
      memset(s->inputbuffer, 0x00, sizeof(s->inputbuffer));
      PSOCK_READTO(&s->p, '\n');

      if(strcmp(s->inputbuffer,"bye\n") == 0)
	break;

      PSOCK_SEND_STR(&s->p, process(s->inputbuffer));
    }

    PSOCK_CLOSE(&s->p);
    PSOCK_END(&s->p);
  }
}
