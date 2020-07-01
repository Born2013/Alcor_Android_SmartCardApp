#ifdef BUILD_LK
#include <debug.h>
#include "lcm_drv.h"
#include <platform/upmu_common.h>
#include <platform/upmu_hw.h>
#include <platform/mt_gpio.h>
#include <platform/mt_i2c.h> 
#include <platform/mt_pmic.h>
#include <string.h>
#include <cust_gpio_usage.h>
#else
#include <linux/string.h>
#include <linux/kernel.h>
#include "lcm_drv.h"
#include <upmu_common.h>
#include <mt_gpio.h>
#include <linux/gpio.h>
#endif

// ---------------------------------------------------------------------------
//  Local Constants
// ---------------------------------------------------------------------------

#define FRAME_WIDTH  										(1080)
#define FRAME_HEIGHT 										(1920)

#define REGFLAG_DELAY             							0XFC
#define REGFLAG_END_OF_TABLE      							0xFD   // END OF REGISTERS MARKER


#define LCM_DSI_CMD_MODE	0


// ---------------------------------------------------------------------------
//  Local Variables
// ---------------------------------------------------------------------------

static LCM_UTIL_FUNCS lcm_util = {0};

#define SET_RESET_PIN(v)    								(lcm_util.set_reset_pin((v)))

#define UDELAY(n) 											(lcm_util.udelay(n))
#define MDELAY(n) 											(lcm_util.mdelay(n))


// ---------------------------------------------------------------------------
//  Local Functions
// ---------------------------------------------------------------------------

#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)	lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update)
#define dsi_set_cmdq(pdata, queue_size, force_update)		lcm_util.dsi_set_cmdq(pdata, queue_size, force_update)
#define wrtie_cmd(cmd)										lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)					lcm_util.dsi_write_regs(addr, pdata, byte_nums)
#define read_reg											lcm_util.dsi_read_reg()
#define read_reg_v2(cmd, buffer, buffer_size)				lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)

static void lcd_power_en(unsigned char enabled)
{
	if (enabled)
	{
		pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x05); //2.8
		pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x01); //1.8
	}	
	else
	{
		pmic_set_register_value(PMIC_RG_VGP1_EN,0);
		pmic_set_register_value(PMIC_RG_VCAMA_EN,0);
	}
	MDELAY(20);

}

struct LCM_setting_table {
    unsigned cmd;
    unsigned char count;
    unsigned char para_list[128];
};


static struct LCM_setting_table lcm_initialization_setting[] = {

{0xFF,1,{0x01}},
{0x6E,1,{0x80}},
{0x68,1,{0x13}},
{0xFB,1,{0x01}},
{0xFF,1,{0x02}},
{0xFB,1,{0x01}},
{0xFF,1,{0x05}},
{0xFB,1,{0x01}},
{0xD7,1,{0x31}},
{0xD8,1,{0x7E}},
{REGFLAG_DELAY, 100, {}},
             
{0xFF,1,{0x00}},
{0xFB,1,{0x01}},
{0xBA,1,{0x03}},
{0x36,1,{0x00}},
{0xB0,1,{0x00}},
{0xD3,1,{0x0a}},
{0xD4,1,{0x0E}},
{0xD5,1,{0x0F}},
{0xD6,1,{0x48}},
{0xD7,1,{0x00}},
{0xD9,1,{0x00}},
{0xFB,1,{0x01}},
{0xFF,1,{0xEE}},
{0x02,1,{0x00}},
{0x40,1,{0x00}},
{0x02,1,{0x00}},
{0x41,1,{0x00}},
{0x02,1,{0x00}},
{0x42,1,{0x00}},
{0xFB,1,{0x01}},
//Command2 Page0
{0xFF,1,{0x01}},
{0xFB,1,{0x01}},
{0x01,1,{0x55}},
{0x04,1,{0x0C}},
{0x05,1,{0x3A}},
{0x06,1,{0x50}},
{0x07,1,{0xD0}},
{0x0A,1,{0x0F}},
{0x0C,1,{0x06}},
{0x0D,1,{0x6B}},
{0x0E,1,{0x6B}},
{0x0F,1,{0x70}},
{0x10,1,{0x63}},
{0x11,1,{0x3C}},
{0x12,1,{0x5C}},
//{0x13,1,{0x50}},  //flick   OTP-VCOM
//{0x14,1,{0x50}},  //flick   OTP-VCOM
{0x15,1,{0x60}},
{0x16,1,{0x15}},
{0x17,1,{0x15}},
{0x5B,1,{0xCA}},
{0x5C,1,{0x00}},
{0x5D,1,{0x00}},
//{0x5E,1,{0x06}},  //vcom/2  OTP-VCOM2
{0x5F,1,{0x1B}},
{0x60,1,{0xD5}},
{0x61,1,{0xF0}}, 
{0x6C,1,{0xAB}},
{0x6D,1,{0x44}},
            
//Command2 Page4
{0xFF,1,{0x05}},
{0xFB,1,{0x01}},
{0x00,1,{0x3F}},
{0x01,1,{0x3F}},
{0x02,1,{0x3F}},
{0x03,1,{0x3F}},
{0x04,1,{0x38}},
{0x05,1,{0x3F}},
{0x06,1,{0x3F}},
{0x07,1,{0x19}},
{0x08,1,{0x1D}},
{0x09,1,{0x3F}},
{0x0A,1,{0x3F}},
{0x0B,1,{0x1B}},
{0x0C,1,{0x17}},
{0x0D,1,{0x3F}},
{0x0E,1,{0x3F}},
{0x0F,1,{0x08}},
{0x10,1,{0x3F}},
{0x11,1,{0x10}},
{0x12,1,{0x3F}},
{0x13,1,{0x3F}},
{0x14,1,{0x3F}},
{0x15,1,{0x3F}},
{0x16,1,{0x3F}},
{0x17,1,{0x3F}},
{0x18,1,{0x38}},
{0x19,1,{0x18}},
{0x1A,1,{0x1C}},
{0x1B,1,{0x3F}},
{0x1C,1,{0x3F}},
{0x1D,1,{0x1A}},
{0x1E,1,{0x16}},
{0x1F,1,{0x3F}},
{0x20,1,{0x3F}},
{0x21,1,{0x3F}},
{0x22,1,{0x3F}},
{0x23,1,{0x06}},
{0x24,1,{0x3F}},
{0x25,1,{0x0E}},
{0x26,1,{0x3F}},
{0x27,1,{0x3F}},
{0x54,1,{0x06}},
{0x55,1,{0x05}},
{0x56,1,{0x04}},
{0x58,1,{0x03}},
{0x59,1,{0x1B}},
{0x5A,1,{0x1B}},
{0x5B,1,{0x01}},
{0x5C,1,{0x32}},
{0x5E,1,{0x18}},
{0x5F,1,{0x20}},
{0x60,1,{0x2B}},
{0x61,1,{0x2C}},
{0x62,1,{0x18}},
{0x63,1,{0x01}},
{0x64,1,{0x32}},
{0x65,1,{0x00}},
{0x66,1,{0x44}},
{0x67,1,{0x11}},
{0x68,1,{0x01}},
{0x69,1,{0x01}},
{0x6A,1,{0x04}},
{0x6B,1,{0x2C}},
{0x6C,1,{0x08}},
{0x6D,1,{0x08}},
{0x78,1,{0x00}},
{0x79,1,{0x00}},
{0x7E,1,{0x00}},
{0x7F,1,{0x00}},
{0x80,1,{0x00}},
{0x81,1,{0x00}},
{0x8D,1,{0x00}},
{0x8E,1,{0x00}},
{0x8F,1,{0xC0}},
{0x90,1,{0x73}},
{0x91,1,{0x10}},
{0x92,1,{0x07}},
{0x96,1,{0x11}},
{0x97,1,{0x14}},
{0x98,1,{0x00}},
{0x99,1,{0x00}},
{0x9A,1,{0x00}},
{0x9B,1,{0x61}},
{0x9C,1,{0x15}},
{0x9D,1,{0x30}},
{0x9F,1,{0x0F}},
{0xA2,1,{0xB0}},
{0xA7,1,{0x0A}},
{0xA9,1,{0x00}},
{0xAA,1,{0x70}},
{0xAB,1,{0xDA}},
{0xAC,1,{0xFF}},
{0xAE,1,{0xF4}},
{0xAF,1,{0x40}},
{0xB0,1,{0x7F}},
{0xB1,1,{0x16}},
{0xB2,1,{0x53}},
{0xB3,1,{0x00}},
{0xB4,1,{0x2A}},
{0xB5,1,{0x3A}},
{0xB6,1,{0xF0}},
{0xBC,1,{0x85}},
{0xBD,1,{0xF4}},
{0xBE,1,{0x33}},
{0xBF,1,{0x13}},
{0xC0,1,{0x77}},
{0xC1,1,{0x77}},
{0xC2,1,{0x77}},
{0xC3,1,{0x77}},
{0xC4,1,{0x77}},
{0xC5,1,{0x77}},
{0xC6,1,{0x77}},
{0xC7,1,{0x77}},
{0xC8,1,{0xAA}},
{0xC9,1,{0x2A}},
{0xCA,1,{0x00}},
{0xCB,1,{0xAA}},
{0xCC,1,{0x92}},
{0xCD,1,{0x00}},
{0xCE,1,{0x18}},
{0xCF,1,{0x88}},
{0xD0,1,{0xAA}},
{0xD1,1,{0x00}},
{0xD2,1,{0x00}},
{0xD3,1,{0x00}},
{0xD6,1,{0x02}},
{0xED,1,{0x00}},
{0xEE,1,{0x00}},
{0xEF,1,{0x70}},
{0xFA,1,{0x03}},
             
//Command2 Page0                                                
{0xFF,1,{0x01}},                 
{0xFB,1,{0x01}},                 
//GAMMA RED+                                                    
{0x75,1,{0x00}},                 
{0x76,1,{0x01}},                 
{0x77,1,{0x00}},                 
{0x78,1,{0x1A}},                 
{0x79,1,{0x00}},                 
{0x7A,1,{0x3D}},                 
{0x7B,1,{0x00}},                 
{0x7C,1,{0x58}},                 
{0x7D,1,{0x00}},                 
{0x7E,1,{0x6E}},                 
{0x7F,1,{0x00}},                 
{0x80,1,{0x81}},                 
{0x81,1,{0x00}},                 
{0x82,1,{0x92}},                 
{0x83,1,{0x00}},                 
{0x84,1,{0xA2}},                 
{0x85,1,{0x00}},                 
{0x86,1,{0xB0}},                 
{0x87,1,{0x00}},                 
{0x88,1,{0xE1}},                 
{0x89,1,{0x01}},                 
{0x8A,1,{0x08}},                 
{0x8B,1,{0x01}},                 
{0x8C,1,{0x47}},                 
{0x8D,1,{0x01}},                 
{0x8E,1,{0x79}},                 
{0x8F,1,{0x01}},                 
{0x90,1,{0xC9}},                 
{0x91,1,{0x02}},                 
{0x92,1,{0x07}},                 
{0x93,1,{0x02}},                 
{0x94,1,{0x09}},                 
{0x95,1,{0x02}},                 
{0x96,1,{0x42}},                 
{0x97,1,{0x02}},                 
{0x98,1,{0x80}},                 
{0x99,1,{0x02}},                 
{0x9A,1,{0xA6}},                 
{0x9B,1,{0x02}},                 
{0x9C,1,{0xDA}},                 
{0x9D,1,{0x02}},                 
{0x9E,1,{0xFB}},                 
{0x9F,1,{0x03}},                 
{0xA0,1,{0x2D}},                 
{0xA2,1,{0x03}},                 
{0xA3,1,{0x3A}},                 
{0xA4,1,{0x03}},                 
{0xA5,1,{0x49}},                 
{0xA6,1,{0x03}},                 
{0xA7,1,{0x5D}},                 
{0xA9,1,{0x03}},                 
{0xAA,1,{0x7A}},                 
{0xAB,1,{0x03}},                 
{0xAC,1,{0x96}},                 
{0xAD,1,{0x03}},                 
{0xAE,1,{0xB4}},                 
{0xAF,1,{0x03}},                 
{0xB0,1,{0xCA}},                 
{0xB1,1,{0x03}},                 
{0xB2,1,{0xCD}},                 
//GAMMA RED-                                                    
{0xB3,1,{0x00}},                 
{0xB4,1,{0x01}},                 
{0xB5,1,{0x00}},                 
{0xB6,1,{0x1A}},                 
{0xB7,1,{0x00}},                 
{0xB8,1,{0x3D}},                 
{0xB9,1,{0x00}},                 
{0xBA,1,{0x58}},                 
{0xBB,1,{0x00}},                 
{0xBC,1,{0x6E}},                 
{0xBD,1,{0x00}},                 
{0xBE,1,{0x81}},                 
{0xBF,1,{0x00}},                 
{0xC0,1,{0x92}},                 
{0xC1,1,{0x00}},                 
{0xC2,1,{0xA2}},                 
{0xC3,1,{0x00}},                 
{0xC4,1,{0xB0}},                 
{0xC5,1,{0x00}},                 
{0xC6,1,{0xE1}},                 
{0xC7,1,{0x01}},                 
{0xC8,1,{0x08}},                 
{0xC9,1,{0x01}},                 
{0xCA,1,{0x47}},                 
{0xCB,1,{0x01}},                 
{0xCC,1,{0x79}},                 
{0xCD,1,{0x01}},                 
{0xCE,1,{0xC9}},                 
{0xCF,1,{0x02}},                 
{0xD0,1,{0x07}},                 
{0xD1,1,{0x02}},                 
{0xD2,1,{0x09}},                 
{0xD3,1,{0x02}},                 
{0xD4,1,{0x42}},                 
{0xD5,1,{0x02}},                 
{0xD6,1,{0x80}},                 
{0xD7,1,{0x02}},                 
{0xD8,1,{0xA6}},                 
{0xD9,1,{0x02}},                 
{0xDA,1,{0xDA}},                 
{0xDB,1,{0x02}},                 
{0xDC,1,{0xFB}},                 
{0xDD,1,{0x03}},                 
{0xDE,1,{0x2D}},                 
{0xDF,1,{0x03}},                 
{0xE0,1,{0x3A}},                 
{0xE1,1,{0x03}},                 
{0xE2,1,{0x49}},                 
{0xE3,1,{0x03}},                 
{0xE4,1,{0x5D}},                 
{0xE5,1,{0x03}},                 
{0xE6,1,{0x7A}},                 
{0xE7,1,{0x03}},                 
{0xE8,1,{0x96}},                 
{0xE9,1,{0x03}},                 
{0xEA,1,{0xB4}},                 
{0xEB,1,{0x03}},                 
{0xEC,1,{0xCA}},                 
{0xED,1,{0x03}},                 
{0xEE,1,{0xCD}},                 
             
//GAMMA Green+                                                  
{0xEF,1,{0x00}},                 
{0xF0,1,{0xA5}},                 
{0xF1,1,{0x00}},                 
{0xF2,1,{0xAC}},                 
{0xF3,1,{0x00}},                 
{0xF4,1,{0xB9}},                 
{0xF5,1,{0x00}},                 
{0xF6,1,{0xC6}},                 
{0xF7,1,{0x00}},                 
{0xF8,1,{0xD1}},                 
{0xF9,1,{0x00}},                 
{0xFA,1,{0xDC}},                 
//Command2 Page1                                                
{0xFF,1,{0x02}},                 
{0xFB,1,{0x01}},                 
{0x00,1,{0x00}},                 
{0x01,1,{0xE8}},                 
{0x02,1,{0x00}},                 
{0x03,1,{0xF1}},                 
{0x04,1,{0x00}},                 
{0x05,1,{0xF9}},                 
{0x06,1,{0x01}},                 
{0x07,1,{0x1C}},                 
{0x08,1,{0x01}},                 
{0x09,1,{0x3C}},                 
{0x0A,1,{0x01}},                 
{0x0B,1,{0x6C}},                 
{0x0C,1,{0x01}},                 
{0x0D,1,{0x95}},                 
{0x0E,1,{0x01}},                 
{0x0F,1,{0xDB}},                 
{0x10,1,{0x02}},                 
{0x11,1,{0x14}},                 
{0x12,1,{0x02}},                 
{0x13,1,{0x16}},                 
{0x14,1,{0x02}},                 
{0x15,1,{0x4C}},                 
{0x16,1,{0x02}},                 
{0x17,1,{0x88}},                 
{0x18,1,{0x02}},                 
{0x19,1,{0xAF}},                 
{0x1A,1,{0x02}},                 
{0x1B,1,{0xE4}},                 
{0x1C,1,{0x03}},                 
{0x1D,1,{0x05}},                 
{0x1E,1,{0x03}},                 
{0x1F,1,{0x3A}},                 
{0x20,1,{0x03}},                 
{0x21,1,{0x49}},                 
{0x22,1,{0x03}},                 
{0x23,1,{0x5A}},                 
{0x24,1,{0x03}},                 
{0x25,1,{0x6C}},                 
{0x26,1,{0x03}},                 
{0x27,1,{0x82}},                 
{0x28,1,{0x03}},                 
{0x29,1,{0x9B}},                 
{0x2A,1,{0x03}},                 
{0x2B,1,{0xB6}},                 
{0x2D,1,{0x03}},                 
{0x2F,1,{0xCA}},                 
{0x30,1,{0x03}},                 
{0x31,1,{0xCD}},                 
//GAMMA Green}},-                                                  
{0x32,1,{0x00}},                 
{0x33,1,{0xA5}},                 
{0x34,1,{0x00}},                 
{0x35,1,{0xAC}},                 
{0x36,1,{0x00}},                 
{0x37,1,{0xB9}},                 
{0x38,1,{0x00}},                 
{0x39,1,{0xC6}},                 
{0x3A,1,{0x00}},                 
{0x3B,1,{0xD1}},                 
{0x3D,1,{0x00}},                 
{0x3F,1,{0xDC}},                 
{0x40,1,{0x00}},                 
{0x41,1,{0xE8}},                 
{0x42,1,{0x00}},                 
{0x43,1,{0xF1}},                 
{0x44,1,{0x00}},                 
{0x45,1,{0xF9}},                 
{0x46,1,{0x01}},                 
{0x47,1,{0x1C}},                 
{0x48,1,{0x01}},                 
{0x49,1,{0x3C}},                 
{0x4A,1,{0x01}},                 
{0x4B,1,{0x6C}},                 
{0x4C,1,{0x01}},                 
{0x4D,1,{0x95}},                 
{0x4E,1,{0x01}},                 
{0x4F,1,{0xDB}},                 
{0x50,1,{0x02}},                 
{0x51,1,{0x14}},                 
{0x52,1,{0x02}},                 
{0x53,1,{0x16}},                 
{0x54,1,{0x02}},                 
{0x55,1,{0x4C}},                 
{0x56,1,{0x02}},                 
{0x58,1,{0x88}},                 
{0x59,1,{0x02}},                 
{0x5A,1,{0xAF}},                 
{0x5B,1,{0x02}},                 
{0x5C,1,{0xE4}},                 
{0x5D,1,{0x03}},                 
{0x5E,1,{0x05}},                 
{0x5F,1,{0x03}},                 
{0x60,1,{0x3A}},                 
{0x61,1,{0x03}},                 
{0x62,1,{0x49}},                 
{0x63,1,{0x03}},                 
{0x64,1,{0x5A}},                 
{0x65,1,{0x03}},                 
{0x66,1,{0x6C}},                 
{0x67,1,{0x03}},                 
{0x68,1,{0x82}},                 
{0x69,1,{0x03}},                 
{0x6A,1,{0x9B}},                 
{0x6B,1,{0x03}},                 
{0x6C,1,{0xB6}},                 
{0x6D,1,{0x03}},                 
{0x6E,1,{0xCA}},                 
{0x6F,1,{0x03}},                 
{0x70,1,{0xCD}},                 
             
//GAMMA Blue+                                                  
{0x71,1,{0x00}},                 
{0x72,1,{0x2C}},                 
{0x73,1,{0x00}},                 
{0x74,1,{0x3B}},                 
{0x75,1,{0x00}},                 
{0x76,1,{0x54}},                 
{0x77,1,{0x00}},                 
{0x78,1,{0x6A}},                 
{0x79,1,{0x00}},                 
{0x7A,1,{0x7B}},                 
{0x7B,1,{0x00}},                 
{0x7C,1,{0x8C}},                 
{0x7D,1,{0x00}},                 
{0x7E,1,{0x9D}},                 
{0x7F,1,{0x00}},                 
{0x80,1,{0xAA}},                 
{0x81,1,{0x00}},                 
{0x82,1,{0xB5}},                 
{0x83,1,{0x00}},                 
{0x84,1,{0xE3}},                 
{0x85,1,{0x01}},                 
{0x86,1,{0x0B}},                 
{0x87,1,{0x01}},                 
{0x88,1,{0x45}},                 
{0x89,1,{0x01}},                 
{0x8A,1,{0x75}},                 
{0x8B,1,{0x01}},                 
{0x8C,1,{0xC5}},                 
{0x8D,1,{0x02}},                 
{0x8E,1,{0x05}},                 
{0x8F,1,{0x02}},                 
{0x90,1,{0x07}},                 
{0x91,1,{0x02}},                 
{0x92,1,{0x41}},                 
{0x93,1,{0x02}},                 
{0x94,1,{0x80}},                 
{0x95,1,{0x02}},                 
{0x96,1,{0xA8}},                 
{0x97,1,{0x02}},                 
{0x98,1,{0xDF}},                 
{0x99,1,{0x03}},                 
{0x9A,1,{0x05}},                 
{0x9B,1,{0x03}},                 
{0x9C,1,{0x49}},                 
{0x9D,1,{0x03}},                 
{0x9E,1,{0x60}},                 
{0x9F,1,{0x03}},                 
{0xA0,1,{0x85}},                 
{0xA2,1,{0x03}},                 
{0xA3,1,{0xA6}},                 
{0xA4,1,{0x03}},                 
{0xA5,1,{0xBC}},                 
{0xA6,1,{0x03}},                 
{0xA7,1,{0xBD}},                 
{0xA9,1,{0x03}},                 
{0xAA,1,{0xBE}},                 
{0xAB,1,{0x03}},                 
{0xAC,1,{0xCA}},                 
{0xAD,1,{0x03}},                 
{0xAE,1,{0xCD}},                 
//GAMMA Blue-                                                   
{0xAF,1,{0x00}},                 
{0xB0,1,{0x2C}},                 
{0xB1,1,{0x00}},                 
{0xB2,1,{0x3B}},                 
{0xB3,1,{0x00}},                 
{0xB4,1,{0x54}},                 
{0xB5,1,{0x00}},                 
{0xB6,1,{0x6A}},                 
{0xB7,1,{0x00}},                 
{0xB8,1,{0x7B}},                 
{0xB9,1,{0x00}},                 
{0xBA,1,{0x8C}},                 
{0xBB,1,{0x00}},                 
{0xBC,1,{0x9D}},                 
{0xBD,1,{0x00}},                 
{0xBE,1,{0xAA}},                 
{0xBF,1,{0x00}},                 
{0xC0,1,{0xB5}},                 
{0xC1,1,{0x00}},                 
{0xC2,1,{0xE3}},                 
{0xC3,1,{0x01}},                 
{0xC4,1,{0x0B}},                 
{0xC5,1,{0x01}},                 
{0xC6,1,{0x45}},                 
{0xC7,1,{0x01}},                 
{0xC8,1,{0x75}},                 
{0xC9,1,{0x01}},                 
{0xCA,1,{0xC5}},                 
{0xCB,1,{0x02}},                 
{0xCC,1,{0x05}},                 
{0xCD,1,{0x02}},                 
{0xCE,1,{0x07}},                 
{0xCF,1,{0x02}},                 
{0xD0,1,{0x41}},                 
{0xD1,1,{0x02}},                 
{0xD2,1,{0x80}},                 
{0xD3,1,{0x02}},                 
{0xD4,1,{0xA8}},                 
{0xD5,1,{0x02}},                 
{0xD6,1,{0xDF}},                 
{0xD7,1,{0x03}},                 
{0xD8,1,{0x05}},                 
{0xD9,1,{0x03}},                 
{0xDA,1,{0x49}},                 
{0xDB,1,{0x03}},                 
{0xDC,1,{0x60}},                 
{0xDD,1,{0x03}},                 
{0xDE,1,{0x85}},                 
{0xDF,1,{0x03}},                 
{0xE0,1,{0xA6}},                 
{0xE1,1,{0x03}},                 
{0xE2,1,{0xBC}},                 
{0xE3,1,{0x03}},                 
{0xE4,1,{0xBD}},                 
{0xE5,1,{0x03}},                 
{0xE6,1,{0xBE}},                 
{0xE7,1,{0x03}},                 
{0xE8,1,{0xCA}},                 
{0xE9,1,{0x03}},                 
{0xEA,1,{0xCD}}, 
                
{0xFF,1,{0x00}},       
{0x11,1,{0x00}},                   
{REGFLAG_DELAY, 150, {}},                                                    
{0x29,1,{0x00}},                  
{REGFLAG_DELAY, 50, {}},                                                   
{REGFLAG_END_OF_TABLE, 0x00, {}}             
};

/*
static struct LCM_setting_table lcm_sleep_out_setting[] = {
	// Sleep Out
	{0x11, 1, {0x00}},
	{REGFLAG_DELAY, 120, {}},

	// Display ON
	{0x29, 1, {0x00}},
	{REGFLAG_DELAY, 20, {}},

	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
*/

static struct LCM_setting_table lcm_deep_sleep_mode_in_setting[] = {
	// Sleep Out
	{0x28, 1, {0x00}},
	{REGFLAG_DELAY, 20, {}},

	// Display ON
	{0x10, 1, {0x00}},
	{REGFLAG_DELAY, 120, {}},

	{REGFLAG_END_OF_TABLE, 0x00, {}}
};

static void push_table(struct LCM_setting_table *table, unsigned int count, unsigned char force_update)
{
	unsigned int i;

    for(i = 0; i < count; i++) {
		
        unsigned cmd;
        cmd = table[i].cmd;
		
        switch (cmd) {
			
            case REGFLAG_DELAY :
                MDELAY(table[i].count);
                break;
				
            case REGFLAG_END_OF_TABLE :
                break;
				
            default:
				dsi_set_cmdq_V2(cmd, table[i].count, table[i].para_list, force_update);
       	}
    }
	
}


// ---------------------------------------------------------------------------
//  LCM Driver Implementations
// ---------------------------------------------------------------------------

static void lcm_set_util_funcs(const LCM_UTIL_FUNCS *util)
{
    memcpy(&lcm_util, util, sizeof(LCM_UTIL_FUNCS));
}


static void lcm_get_params(LCM_PARAMS *params)
{
		memset(params, 0, sizeof(LCM_PARAMS));
	
		params->type   = LCM_TYPE_DSI;

		params->width  = FRAME_WIDTH;
		params->height = FRAME_HEIGHT;

		// enable tearing-free
		params->dbi.te_mode 			= LCM_DBI_TE_MODE_DISABLED;  //LCM_DBI_TE_MODE_VSYNC_ONLY;
		params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

		params->dsi.mode   = BURST_VDO_MODE;

	
		// DSI
		/* Command mode setting */
		params->dsi.LANE_NUM		    = LCM_FOUR_LANE;
		//The following defined the fomat for data coming from LCD engine.
		params->dsi.data_format.color_order = LCM_COLOR_ORDER_RGB;
		params->dsi.data_format.trans_seq   = LCM_DSI_TRANS_SEQ_MSB_FIRST;
		params->dsi.data_format.padding     = LCM_DSI_PADDING_ON_LSB;
		params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

		// Highly depends on LCD driver capability.
		// Not support in MT6573
		params->dsi.packet_size=256;

		// Video mode setting		
		//params->dsi.intermediat_buffer_num = 2;

		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;

		params->dsi.vertical_sync_active				= 2;//2;
		params->dsi.vertical_backporch					= 8;   // from Q driver
		params->dsi.vertical_frontporch					= 14;  // rom Q driver
		params->dsi.vertical_active_line				= FRAME_HEIGHT;

		params->dsi.horizontal_sync_active				= 20;//10;
		params->dsi.horizontal_backporch				= 80; // from Q driver
		params->dsi.horizontal_frontporch				= 80;  // from Q driver
		params->dsi.horizontal_active_pixel				= FRAME_WIDTH;


		params->dsi.PLL_CLOCK = 445;//450; //this value must be in MTK suggested table
		params->dsi.ssc_disable	= 1;



#if 1
		params->dsi.esd_check_enable 			= 1;
		params->dsi.customization_esd_check_enable 	= 1;
		params->dsi.lcm_esd_check_table[0].cmd 		= 0x0A;//0xac;
		params->dsi.lcm_esd_check_table[0].count 	= 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] = 0x9C;//0x00;
		params->dsi.noncont_clock = 1;
		params->dsi.noncont_clock_period = 2;
#endif
}




static void lcm_init(void)
{

	lcd_power_en(1);
	MDELAY(100);
	SET_RESET_PIN(1);
	SET_RESET_PIN(0);
	MDELAY(10);
	SET_RESET_PIN(1);
	MDELAY(10);

	push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);

}


static void lcm_suspend(void)
{
	SET_RESET_PIN(1);
	SET_RESET_PIN(0);
	MDELAY(10);
	SET_RESET_PIN(1);
	push_table(lcm_deep_sleep_mode_in_setting, sizeof(lcm_deep_sleep_mode_in_setting) / sizeof(struct LCM_setting_table), 1);
}


static void lcm_resume(void)
{

	lcm_init();
	//push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);
}


static unsigned int lcm_compare_id(void)
{
	unsigned int lcd_id1,lcd_id2 = 0;
	lcd_power_en(1);
	MDELAY(50);
#ifdef BUILD_LK
	lcd_id1 = mt_get_gpio_in(GPIO_LCD_ID1_PIN);
	lcd_id2 = mt_get_gpio_in(GPIO_LCD_ID2_PIN);
#else
	lcd_id1 = gpio_get_value(GPIO19_LCD_ID1_PIN);
	lcd_id2 = gpio_get_value(GPIO20_LCD_ID2_PIN);
#endif

	if((lcd_id1==0)&&(lcd_id2==1))
		return 1;
	else
		return 0;
}

LCM_DRIVER nt35532_fhd_dsi_vdo_panda_yixinda_for_w5503_lcm_drv = 
{
      	.name		= "nt35532_fhd_dsi_vdo_panda_yixinda_for_w5503",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
};
