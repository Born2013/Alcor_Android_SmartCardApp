
#include "lcm_drv.h"

#ifdef BUILD_LK
#include <platform/mt_gpio.h>
#else
#include <linux/gpio.h>
#endif

// ---------------------------------------------------------------------------
//  Local Constants
// ---------------------------------------------------------------------------

#define FRAME_WIDTH  										(480)
#define FRAME_HEIGHT 										(800)

#define REGFLAG_DELAY             							0XFE
#define REGFLAG_END_OF_TABLE      							0xF1  // END OF REGISTERS MARKER

#define LCM_DSI_CMD_MODE								0

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
#define wrtie_cmd(cmd)									lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)				lcm_util.dsi_write_regs(addr, pdata, byte_nums)
//#define read_reg(cmd)											lcm_util.DSI_dcs_read_lcm_reg(cmd)
#define read_reg_v2(cmd, buffer, buffer_size)   				lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)    

struct LCM_setting_table {
    unsigned char cmd;
    unsigned char count;
    unsigned char para_list[64];
};


// LCD 晶泰 ILI9806E video
static struct LCM_setting_table lcm_initialization_setting[] = {
 	//ILI9806E_CTC3.97_TN  initial

	{0xFF,5,{0xFF,0x98,0x06,0x04,0x01}},     // Change to Page 1
	{0x08,1,{0x10}},
	{0x21,1,{0x01}},
	{0x30,1,{0x02}},                 // 480 X 800
	{0x31,1,{0x02}},
	{0x40,1,{0x1A}},//10
	{0x41,1,{0x77}},//22
	{0x42,1,{0x02}},
	{0x43,1,{0x09}},
	{0x44,1,{0x06}},//82
	{0x50,1,{0x78}},
	{0x51,1,{0x78}},
	{0x52,1,{0x00}},  // flicker MSB
	{0x53,1,{0x6B}},  // flicker LSB /2D
//	{0x57,1,{0x50}},
	{0x60,1,{0x14}},
	{0x61,1,{0x00}},
	{0x62,1,{0x08}},
	{0x63,1,{0x00}},

	//++++++++++++++++++ Gamma Setting ++++++++++++++++++//
	{0xA0,1,{0x00}},  // Gamma 0
	{0xA1,1,{0x03}},  // Gamma 4
	{0xA2,1,{0x0c}},  // Gamma 8
	{0xA3,1,{0x10}},  // Gamma 16
	{0xA4,1,{0x0a}},  // Gamma 24
	{0xA5,1,{0x16}},  // Gamma 52
	{0xA6,1,{0x08}},  // Gamma 80
	{0xA7,1,{0x05}},  // Gamma 108
	{0xA8,1,{0x07}},  // Gamma 147
	{0xA9,1,{0x0c}},  // Gamma 175
	{0xAA,1,{0x09}},  // Gamma 203
	{0xAB,1,{0x06}},  // Gamma 231
	{0xAC,1,{0x18}},  // Gamma 239
	{0xAD,1,{0x2e}},  // Gamma 247
	{0xAE,1,{0x33}},  // Gamma 251
	{0xAF,1,{0x08}},  // Gamma 255
	{0xC0,1,{0x00}},  // Gamma 0
	{0xC1,1,{0x05}},  // Gamma 4
	{0xC2,1,{0x0c}},  // Gamma 8
	{0xC3,1,{0x10}},  // Gamma 16
	{0xC4,1,{0x09}},  // Gamma 24
	{0xC5,1,{0x19}},  // Gamma 52
	{0xC6,1,{0x0A}},  // Gamma 80
	{0xC7,1,{0x09}},  // Gamma 108
	{0xC8,1,{0x00}},  // Gamma 147
	{0xC9,1,{0x05}},  // Gamma 175
	{0xCA,1,{0x04}},  // Gamma 203
	{0xCB,1,{0x01}},  // Gamma 231  
	{0xCC,1,{0x00}},  // Gamma 239
	{0xCD,1,{0x2c}},  // Gamma 247
	{0xCE,1,{0x1e}},  // Gamma 251
	{0xCF,1,{0x08}},  // Gamma 255
	{0xFF,5,{0xFF,0x98,0x06,0x04,0x06}},     // Change to Page 6
	{0x00,1,{0x23}},
	{0x01,1,{0x0A}},
	{0x02,1,{0x00}},
	{0x03,1,{0x04}},
	{0x04,1,{0x01}},
	{0x05,1,{0x01}},
	{0x06,1,{0x80}},
	{0x07,1,{0x08}},
	{0x08,1,{0x0B}},
	{0x09,1,{0x00}},
	{0x0A,1,{0x00}},
	{0x0B,1,{0x00}},
	{0x0C,1,{0x01}},
	{0x0D,1,{0x01}},
	{0x0E,1,{0x00}},
	{0x0F,1,{0x00}},
	{0x10,1,{0x7E}},
	{0x11,1,{0xF3}},
	{0x12,1,{0x04}},
	{0x13,1,{0x00}},
	{0x14,1,{0x00}},
	{0x15,1,{0xC0}},
	{0x16,1,{0x08}},
	{0x17,1,{0x00}},
	{0x18,1,{0x00}},
	{0x19,1,{0x00}},
	{0x1A,1,{0x00}},
	{0x1B,1,{0x00}},
	{0x1C,1,{0x00}},
	{0x1D,1,{0x00}},
	{0x20,1,{0x01}},
	{0x21,1,{0x23}},
	{0x22,1,{0x45}},
	{0x23,1,{0x67}},
	{0x24,1,{0x01}},
	{0x25,1,{0x23}},
	{0x26,1,{0x45}},
	{0x27,1,{0x67}},
	{0x30,1,{0x01}},
	{0x31,1,{0x22}},
	{0x32,1,{0x22}},
	{0x33,1,{0x86}},
	{0x34,1,{0xBA}},
	{0x35,1,{0xAB}},
	{0x36,1,{0xDC}},
	{0x37,1,{0xCD}},
	{0x38,1,{0x22}},
	{0x39,1,{0xEE}},
	{0x3A,1,{0xFF}},
	{0x3B,1,{0x22}},
	{0x3C,1,{0x68}},
	{0x3D,1,{0x22}},
	{0x3E,1,{0x22}},
	{0x3F,1,{0x99}},
	{0x40,1,{0x22}},
	{0x52,1,{0x10}},
	{0x53,1,{0x10}},
	{0xFF,5,{0xFF,0x98,0x06,0x04,0x07}},    // Change to Page 7
	{0x17,1,{0x22}},
	{0x02,1,{0x77}},
	{0xE1,1,{0x79}},
	{0x26,1,{0xB2}},
	{0xFF,5,{0xFF,0x98,0x06,0x04,0x00}},     // Change to Page 0
	{0x35,1,{0x00}},
	{0x11,1,{0x00}},
	{REGFLAG_DELAY, 120, {}},
	{0x29,1,{0x00}}, 
	{REGFLAG_DELAY, 10, {}},


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
                MDELAY(1);

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
		params->dbi.te_mode 				= LCM_DBI_TE_MODE_DISABLED;  //LCM_DBI_TE_MODE_VSYNC_ONLY;
		//params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

		params->dsi.mode   = SYNC_PULSE_VDO_MODE; //BURST_VDO_MODE;//

		// DSI
		/* Command mode setting */
		params->dsi.LANE_NUM				= LCM_TWO_LANE;
		//The following defined the fomat for data coming from LCD engine.
		params->dsi.data_format.color_order = LCM_COLOR_ORDER_RGB;
		params->dsi.data_format.trans_seq   = LCM_DSI_TRANS_SEQ_MSB_FIRST;  //MSB
		params->dsi.data_format.padding     = LCM_DSI_PADDING_ON_LSB;
		params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

		// Highly depends on LCD driver capability.
		// Not support in MT6573
		params->dsi.packet_size=256;

		// Video mode setting		
		params->dsi.intermediat_buffer_num = 2;

		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;

		params->dsi.vertical_sync_active				= 4;
		params->dsi.vertical_backporch					= 16;
		params->dsi.vertical_frontporch					= 20;
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		//params->dsi.horizontal_sync_active				= 10+10;
		//params->dsi.horizontal_backporch				= 64+20;
		//params->dsi.horizontal_frontporch				= 64+20;
		params->dsi.horizontal_sync_active				= 10;
		params->dsi.horizontal_backporch				= 80;
		params->dsi.horizontal_frontporch				= 80;
		params->dsi.horizontal_active_pixel				= FRAME_WIDTH;

		// Bit rate calculation
#if defined(BIRD_LCM_CLOCK_W18)
	params->dsi.PLL_CLOCK = 185;
#elif defined(BIRD_LCM_CLOCK_W2)
	params->dsi.PLL_CLOCK = 215;
#else
	params->dsi.PLL_CLOCK = 215;//216;//217;//215;//213;//192;//208 241 280 208  //  /******* 215  850  1900 OK*******2016-5-5***/
#endif
   	
	// params->dsi.cont_clock=1;
		params->dsi.ssc_disable	= 1;
		//params->dsi.ssc_range	= 2;
	//params->dsi.clk_lp_per_line_enable = 0;
}


static void lcm_init(void)
{
	SET_RESET_PIN(1);
	MDELAY(10);
	SET_RESET_PIN(0);
	MDELAY(30);
	SET_RESET_PIN(1);
	MDELAY(100);
	push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);
}


static void lcm_suspend(void)
{
	unsigned int data_array[16];

	data_array[0]=0x00280500; // Display Off
	dsi_set_cmdq(data_array, 1, 1);
	MDELAY(10); 
	data_array[0] = 0x00100500; // Sleep In
	dsi_set_cmdq(data_array, 1, 1);
	MDELAY(120);
	
}

static void lcm_resume(void)
{
/*	unsigned int data_array[16];

//lcm_initialization_setting[12].para_list[0]+=2;
	data_array[0] = 0x00110500; // Sleep Out
	dsi_set_cmdq(data_array, 1, 1);
	MDELAY(150);
	data_array[0] = 0x00290500; // Display On
	dsi_set_cmdq(data_array, 1, 1);
	MDELAY(10);
*/
	lcm_init();

}
   
static unsigned int lcm_compare_id(void)
{
	unsigned int lcd_id1,lcd_id2 = 0;

#ifdef BUILD_LK
	lcd_id1 = mt_get_gpio_in(GPIO_LCD_ID1_PIN);
	lcd_id2 = mt_get_gpio_in(GPIO_LCD_ID2_PIN);
#else
	lcd_id1 = gpio_get_value(GPIO19_LCD_ID1_PIN);
	lcd_id2 = gpio_get_value(GPIO20_LCD_ID2_PIN);
#endif
	if((lcd_id1==0)&&(lcd_id2==0))
		return 1;
	else
		return 0;
}

LCM_DRIVER ili9806e_wvga_cmi_vdo_yixinda_for_e4008_lcm_drv = 
{
    	.name			= "ili9806e_wvga_cmi_vdo_yixinda_for_e4008",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
};
