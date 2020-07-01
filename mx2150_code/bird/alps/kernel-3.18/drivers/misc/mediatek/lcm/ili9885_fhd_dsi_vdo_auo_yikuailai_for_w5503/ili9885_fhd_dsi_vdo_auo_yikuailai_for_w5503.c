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

	{0xB0,3,{0x98,0x85,0x0A}},
	{REGFLAG_DELAY, 20, {}},

	{0xC4,7,{0x70,0x19,0x23,0x00,0x0F,0x0F,0x00}}, 		 //set SDT
	{0xD0,6,{0x55,0x05,0x34,0x6B,0xEC,0xC0}}, 		 //VGH=3AVDD-AVEE=16V(single), VGL=2AVEE-AVDD=-14V(dual)
	{0xD3,9,{0x33,0x33,0x05,0x03,0x59,0x59,0x22,0x26,0x22}}, //GVDDP=4.3V GVDDN=-4.3V VGHO=15 VGLO=-12 AVDDR=4.7V AVEER=-4.7V
	//{0xD5,10,{0x8B,0x00,0x00,0x00,0x01,0x8A,0x01,0x8A,0x00,0xFF }},//set Vcom

	{0xEC,7,{0x76,0x1E,0x32,0x00,0x46,0x00,0x02}}, 	//black display while video stop

	{0xEF,1,{0x8F}}, 					//power saving

	//set LVD sequence
	{0xEB,35,{0xA3,0xC7,0x73,0x00,0x58,0x55,0x55,0x55,0x55,0x50,
		0x00,0x00,0x00,0x00,0x00,0x25,0xCD,0x0F,0xFF,0xFF,
		0xFF,0xFF,0xFF,0x55,0x55,0x55,0x55,0x32,0x77,0x55,
		0x43,0x45,0x5E,0xFF,0x55}}, 			//VCOM tie GND during sleep in & source GND for 2 frames

	//GIP setting
	{0xE5,73,{0x36,0x36,0xA1,0xF6,0xF6,0x47,0x07,0x55,0x15,0x63,
		0x23,0x71,0x31,0x3E,0x37,0x85,0x36,0x36,0x36,0x36,
		0x36,0x36,0xA8,0xF6,0xF6,0x4E,0x0E,0x5C,0x1C,0x6A,
		0x2A,0x78,0x38,0x3E,0x37,0x8C,0x36,0x36,0x36,0x36,
		0x18,0x70,0x61,0x00,0x4E,0xBB,0x70,0x80,0x00,0x4E,
		0xBB,0xF7,0x00,0x4E,0xBB,0x00,0x00,0x00,0x00,0x00,
		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x07}},

	{0xEA,66,{0x51,0x00,0x00,0x00,0x00,0x00,0x00,0x40,0x00,0x00,
		0x00,0x00,0x00,0x00,0x0F,0x00,0x00,0x00,0x00,0x00,
		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x70,
		0x01,0x10,0x00,0x40,0x80,0xC0,0x00,0x00,0x01,0x23,
		0x45,0x67,0x89,0xAB,0xCD,0xEF,0xCC,0xCC,0x22,0x33,
		0x33,0x00,0x11,0x00,0x11,0x00,0x11,0x00,0x11,0xCC,
		0xCC,0x22,0xCC,0xCC,0xCC,0xCC}},

	{0xED,23,{0x00,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x40}}, //setreg_dispon_wait_vdo                                 

	//gamma setting
	{0xC7,122,{0x00,0x19,0x00,0x68,0x00,0x96,0x00,0xB0,0x00,0xD0,
		0x00,0xE2,0x00,0xF2,0x01,0x04,0x01,0x14,0x01,0x43,
		0x01,0x66,0x01,0x9F,0x01,0xCB,0x02,0x10,0x02,0x45,
		0x02,0x47,0x02,0x7A,0x02,0xB1,0x02,0xD5,0x03,0x06,
		0x03,0x24,0x03,0x48,0x03,0x58,0x03,0x61,0x03,0x73,
		0x03,0x7D,0x03,0x93,0x03,0xA8,0x03,0xC8,0x03,0xE8,
		0x00,0x19,0x00,0x68,0x00,0x96,0x00,0xB0,0x00,0xD0,
		0x00,0xE2,0x00,0xF2,0x01,0x04,0x01,0x14,0x01,0x43,
		0x01,0x66,0x01,0x9F,0x01,0xCB,0x02,0x10,0x02,0x45,
		0x02,0x47,0x02,0x7A,0x02,0xB1,0x02,0xD5,0x03,0x06,
		0x03,0x24,0x03,0x48,0x03,0x58,0x03,0x61,0x03,0x73,
		0x03,0x7D,0x03,0x93,0x03,0xA8,0x03,0xC8,0x03,0xE8,0x01,0x01}},

	{0xD1,3,{0x09,0x09,0xC2}},   

	{0xEE,14,{0x22,0x10,0x02,0x02,0x0F,0x40,0x00,0xA7,0x00,0x04,0x00,0x00,0x40,0xB9}},    


	{0x11,1,{0x00}},
	{REGFLAG_DELAY, 120, {}},
	{0x29,1,{0x00}},
	{REGFLAG_DELAY,20,{}}, 			  // Display On
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

		params->dsi.vertical_sync_active				= 6;//2;
		params->dsi.vertical_backporch					= 18;   // from Q driver
		params->dsi.vertical_frontporch					= 20;  // rom Q driver
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
		params->dsi.lcm_esd_check_table[0].cmd 		= 0xac;//0x0A;
		params->dsi.lcm_esd_check_table[0].count 	= 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] = 0x00;//0x9C;
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

	if((lcd_id1==0)&&(lcd_id2==0))
		return 1;
	else
		return 0;
}

LCM_DRIVER ili9885_fhd_dsi_vdo_auo_yikuailai_for_w5503_lcm_drv = 
{
      	.name		= "ili9885_fhd_dsi_vdo_auo_yikuailai_for_w5503",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
};
