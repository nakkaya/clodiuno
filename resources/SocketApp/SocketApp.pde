/*
 * Socket App
 *
 * A simple socket application example using the WiShield 1.0
 */

#include <WiShield.h>

#define WIRELESS_MODE_INFRA	1
#define WIRELESS_MODE_ADHOC	2

// Wireless configuration parameters ----------------------------------------
// IP address of WiShield
unsigned char local_ip[] = {10,0,2,100};
// router or gateway IP address
unsigned char gateway_ip[] = {10,0,2,1};
// subnet mask for the local network
unsigned char subnet_mask[] = {255,255,255,0};
// max 32 bytes
const prog_char ssid[] PROGMEM = {"hole"};


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
