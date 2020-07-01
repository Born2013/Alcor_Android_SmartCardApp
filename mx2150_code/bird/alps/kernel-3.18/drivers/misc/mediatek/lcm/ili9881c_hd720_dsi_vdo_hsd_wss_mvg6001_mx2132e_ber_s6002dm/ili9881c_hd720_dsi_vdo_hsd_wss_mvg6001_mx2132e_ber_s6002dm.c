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

#define FRAME_WIDTH  (720)
#define FRAME_HEIGHT (1280)

#define REGFLAG_DELAY             							0XFC
#define REGFLAG_END_OF_TABLE      							0xFD   // END OF REGISTERS MARKER

#define LCM_ID	

#define LCM_DSI_CMD_MODE									0

#ifndef TRUE
    #define TRUE 1
#endif

#ifndef FALSE
    #define FALSE 0
#endif

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

		//pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		//pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x05);
		//pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		//pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x01);
	}	
	else
	{
		//pmic_set_register_value(PMIC_RG_VGP1_EN,0);
		//pmic_set_register_value(PMIC_RG_VCAMA_EN,0);
	}
	MDELAY(20);

}


struct LCM_setting_table {
    unsigned cmd;
    unsigned char count;
    unsigned char para_list[64];
};

#if 0
static struct LCM_setting_table lcm_set_window[] = {
	{0x2A,	4,	{0x00, 0x00, (FRAME_WIDTH>>8), (FRAME_WIDTH&0xFF)}},
	{0x2B,	4,	{0x00, 0x00, (FRAME_HEIGHT>>8), (FRAME_HEIGHT&0xFF)}},
	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
#endif
#if 0
static struct LCM_setting_table lcm_sleep_out_setting[] = {
    // Sleep Out
	{0x11, 1, {0x00}},
    {REGFLAG_DELAY, 120, {}},
    
	//{0x36,1,{0x08}}, //P1

    // Display ON
	{0x29, 1, {0x00}},
	{REGFLAG_DELAY, 10, {}},
	
	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
#endif

static struct LCM_setting_table lcm_sleep_in_setting[] = {
	// Display off sequence
	{0x28, 1, {0x00}},
	{REGFLAG_DELAY, 10, {}},
    // Sleep Mode On
	{0x10, 1, {0x00}},
	{REGFLAG_DELAY, 120, {}},

	{REGFLAG_END_OF_TABLE, 0x00, {}}
};



static struct LCM_setting_table lcm_initialization_setting[] = {
	{0xFF,3,{0x98,0x81,0x03}},
																		
	{0x01,1,{0x00}},
	{0x02,1,{0x00}},
	{0x03,1,{0x73}},//
	{0x04, 1 , {0x00}},
	{0x05, 1 , {0x00}},
	{0x06, 1 , {0x0A}},
	{0x07, 1 , {0x00}},
	{0x08, 1 , {0x00}},
	{0x09, 1 , {0x01}},
	{0x0a, 1 , {0x00}},
	{0x0b, 1 , {0x00}},
	{0x0c, 1 , {0x01}},
	{0x0d, 1 , {0x00}},
	{0x0e, 1 , {0x00}},
	{0x0f, 1 , {0x13}},
	{0x10, 1 , {0x13}},
	{0x11, 1 , {0x00}},
	{0x12, 1 , {0x00}},
	{0x13, 1 , {0x00}},
	{0x14,1,{0x00}},
	{0x15,1,{0x00}},//
	{0x16,1,{0x00}},
	{0x17,1,{0x00}},
	{0x18,1,{0x00}},
	{0x19,1,{0x00}},
	{0x1a,1,{0x00}},
	{0x1b,1,{0x00}},
	{0x1c,1,{0x00}},
	{0x1d,1,{0x00}},
	{0x1e, 1 , {0x40}},
	{0x1f, 1 , {0x80}},
	{0x20, 1 , {0x06}},
	{0x21, 1 , {0x01}},
	{0x22, 1 , {0x00}},
	{0x23, 1 , {0x00}},
	{0x24, 1 , {0x00}},
	{0x25, 1 , {0x00}},
	{0x26, 1 , {0x00}},
	{0x27, 1 , {0x00}},
	{0x28, 1 , {0x33}},
	{0x29, 1 , {0x03}},
	{0x2a,1,{0x00}},
	{0x2b,1,{0x00}},
	{0x2c,1,{0x00}},//
	{0x2d,1,{0x00}},
	{0x2e,1,{0x00}},
	{0x2f,1,{0x00}},
	{0x30,1,{0x00}},
	{0x31,1,{0x00}},
	{0x32,1,{0x00}},
	{0x33,1,{0x00}},//
	{0x34, 1 , {0x04}},
	{0x35, 1 , {0x00}},
	{0x36, 1 , {0x00}},
	{0x37, 1 , {0x00}},
	{0x38, 1 , {0x78}},
	{0x39,1,{0x00}},
	{0x3a,1,{0x00}},
	{0x3b,1,{0x00}},
	{0x3c,1,{0x00}},
	{0x3d,1,{0x00}},
	{0x3e,1,{0x00}},
	{0x3f,1,{0x00}},
	{0x40,1,{0x00}},
	{0x41,1,{0x00}},
	{0x42,1,{0x00}},
	{0x43,1,{0x00}},
	{0x44,1,{0x00}},


	{0x50, 1 , {0x10}},
	{0x51, 1 , {0x32}},
	{0x52, 1 , {0x54}},
	{0x53, 1 , {0x76}},
	{0x54, 1 , {0x98}},
	{0x55, 1 , {0xBA}},
	{0x56, 1 , {0x10}},
	{0x57, 1 , {0x32}},
	{0x58, 1 , {0x54}},
	{0x59, 1 , {0x76}},
	{0x5a, 1 , {0x98}},
	{0x5b, 1 , {0xBA}},
	{0x5c, 1 , {0xDC}},
	{0x5d, 1 , {0xFE}},
	{0x5e, 1 , {0x00}},
	{0x5f, 1 , {0x0E}},
	{0x60, 1 , {0x0F}},
	{0x61, 1 , {0x0C}},
	{0x62, 1 , {0x0D}},
	{0x63, 1 , {0x06}},
	{0x64, 1 , {0x07}},
	{0x65, 1 , {0x02}},
	{0x66, 1 , {0x02}},
	{0x67, 1 , {0x02}},
	{0x68, 1 , {0x02}},
	{0x69, 1 , {0x01}},
	{0x6a, 1 , {0x00}},
	{0x6b, 1 , {0x02}},
	{0x6c, 1 , {0x15}},
	{0x6d, 1 , {0x14}},
	{0x6e, 1 , {0x02}},
	{0x6f, 1 , {0x02}},
	{0x70, 1 , {0x02}},
	{0x71, 1 , {0x02}},
	{0x72, 1 , {0x02}},
	{0x73, 1 , {0x02}},
	{0x74, 1 , {0x02}},
	{0x75, 1 , {0x0E}},
	{0x76, 1 , {0x0F}},
	{0x77, 1 , {0x0C}},
	{0x78, 1 , {0x0D}},
	{0x79, 1 , {0x06}},
	{0x7a, 1 , {0x07}},
	{0x7b, 1 , {0x02}},
	{0x7c, 1 , {0x02}},
	{0x7d, 1 , {0x02}},
	{0x7e, 1 , {0x02}},
	{0x7f, 1 , {0x01}},
	{0x80, 1 , {0x00}},
	{0x81, 1 , {0x02}},
	{0x82, 1 , {0x14}},
	{0x83, 1 , {0x15}},
	{0x84, 1 , {0x02}},
	{0x85, 1 , {0x02}},
	{0x86, 1 , {0x02}},
	{0x87, 1 , {0x02}},
	{0x88, 1 , {0x02}},
	{0x89, 1 , {0x02}},
	{0x8a, 1 , {0x02}},
			
	{0xFF,3,{0x98,0x81,0x04}},
    //{0x00,1,{0x00}},
	//{0x7a,1,{0x10}},
	{0x6C,1,{0x15}},                
	{0x6e, 1 , {0x2a}},
	{0x6f, 1 , {0x33}}, 
	{0x3A, 1 , {0x94}}, //94
	{0x8D, 1 , {0x1a}}, //1a
	{0x87, 1 , {0xBA}},
	{0x26, 1 , {0x76}},
	{0xB2, 1 , {0xD1}},  
	{0xB5, 1 , {0x06}},
	//{0x88, 1 , {0x0b}},
	{0xFF, 3 , {0x98,0x81,0x01}},
	{0x22, 1 , {0x0A}},
	{0x31, 1 , {0x00}},	
	{0x53, 1 , {0x72}},
	{0x55, 1 , {0x8F}},
	{0x50, 1 , {0x96}},
	{0x51, 1 , {0x96}},
	{0x60, 1 , {0x08}},
	{0xA0, 1 , {0x08}},
	{0xA1, 1 , {0x15}},
	{0xA2, 1 , {0x22}},
	{0xA3, 1 , {0x14}},
	{0xA4, 1 , {0x17}},
	{0xA5, 1 , {0x29}},
	{0xA6, 1 , {0x1e}},
	{0xA7, 1 , {0x1d}},
	{0xA8, 1 , {0x79}},
	{0xA9, 1 , {0x21}},
	{0xAa, 1 , {0x2b}},
	{0xAb, 1 , {0x69}},
	{0xAc, 1 , {0x1a}},
	{0xAd, 1 , {0x19}},
	{0xAe, 1 , {0x4b}},
	{0xAf, 1 , {0x22}},
	{0xB0, 1 , {0x26}},
	{0xB1, 1 , {0x4a}},
	{0xB2, 1 , {0x64}},
	{0xB3, 1 , {0x39}},
	{0xc0, 1 , {0x08}},
	{0xc1, 1 , {0x14}},
	{0xc2, 1 , {0x1e}},
	{0xc3, 1 , {0x0d}},
	{0xc4, 1 , {0x0f}},
	{0xc5, 1 , {0x20}},
	{0xc6, 1 , {0x15}},
	{0xc7, 1 , {0x1a}},
	{0xc8, 1 , {0x6a}},
	{0xc9, 1 , {0x18}},
	{0xca, 1 , {0x24}},
	{0xcb, 1 , {0x5f}},
	{0xcc, 1 , {0x1d}},
	{0xcd, 1 , {0x1B}},
	{0xce, 1 , {0x51}}, 
	{0xcf, 1 , {0x21}}, 
	{0xd0, 1 , {0x2b}},
	{0xd1, 1 , {0x49}},
	{0xd2, 1 , {0x62}},
	{0xd3, 1 , {0x39}},
	
	{0xFF,3,{0x98,0x81,0x00}},
	//{0x35,1,{0x00}},  
	//{0x3A,1,{0x77}},  
	{0x11,1,{0x00}},
	{REGFLAG_DELAY,120,{}},
		                         				                                                                                
	{0x29,1,{0x00}},//Display ON 
	{REGFLAG_DELAY,20,{}},	


    {REGFLAG_END_OF_TABLE, 0x00, {}}
};


static void push_table(struct LCM_setting_table *table, unsigned int count, unsigned char force_update)
{
    unsigned int i;

    for(i = 0; i < count; i++)
    {

        unsigned cmd;
        cmd = table[i].cmd;

        switch (cmd)
        {

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

	params->type = LCM_TYPE_DSI;

	params->width = FRAME_WIDTH;
	params->height = FRAME_HEIGHT;

		// enable tearing-free
		params->dbi.te_mode 				= LCM_DBI_TE_MODE_DISABLED;  //LCM_DBI_TE_MODE_VSYNC_ONLY;
		//params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

		params->dsi.mode   = SYNC_PULSE_VDO_MODE;

	
		// DSI
		/* Command mode setting */
		params->dsi.LANE_NUM				= LCM_FOUR_LANE;
		//The following defined the fomat for data coming from LCD engine.
		params->dsi.data_format.color_order = LCM_COLOR_ORDER_RGB;
		params->dsi.data_format.trans_seq   = LCM_DSI_TRANS_SEQ_MSB_FIRST;
		params->dsi.data_format.padding     = LCM_DSI_PADDING_ON_LSB;
		params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

		// Highly depends on LCD driver capability.
		// Not support in MT6573
		params->dsi.packet_size=256;

		params->dsi.intermediat_buffer_num = 0;

		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;
		params->dsi.word_count=720*3;	

		
		params->dsi.vertical_sync_active					= 8;//8;
		params->dsi.vertical_backporch					= 8;//8;
		params->dsi.vertical_frontporch					= 20;//10;
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		params->dsi.horizontal_sync_active				= 60;//10;
		params->dsi.horizontal_backporch				= 20;//20;
		params->dsi.horizontal_frontporch				= 100;//40;
		params->dsi.horizontal_active_pixel				= FRAME_WIDTH;
		params->dsi.esd_check_enable 			= 1;
		params->dsi.customization_esd_check_enable 	= 1;
		params->dsi.lcm_esd_check_table[0].cmd            = 0x0A;
		params->dsi.lcm_esd_check_table[0].count        = 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] = 0x9C;

		params->dsi.HS_TRAIL = 15;  //new add
		params->dsi.PLL_CLOCK = 210;//220//240; //this value must be in MTK suggested table

}


static void lcm_init(void)
{
	lcd_power_en(1);
	MDELAY(50);
	SET_RESET_PIN(1);
	MDELAY(2);
	SET_RESET_PIN(0);
	MDELAY(10);
	SET_RESET_PIN(1);
	MDELAY(100);
	
	push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);

}



static void lcm_suspend(void)
{

        push_table(lcm_sleep_in_setting, sizeof(lcm_sleep_in_setting) / sizeof(struct LCM_setting_table), 1);
		
	SET_RESET_PIN(0);
	MDELAY(10);
	SET_RESET_PIN(1);
  	MDELAY(20);

}


static void lcm_resume(void)
{
#ifndef BUILD_LK
printk("crystal_ili9881c_hd720_dsi_vdo_hsd_wss_mvg6001_mx2132e_ber_s6002\n");
#endif
lcm_init();
//push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);
}
         




static unsigned int lcm_compare_id(void)
{
#ifdef BUILD_LK
	unsigned int lcd_id = 0;

	lcd_power_en(1);
	MDELAY(50);
   	lcd_id = (mt_get_gpio_in(GPIO_LCD_ID1_PIN));

	printf("lcd_id anan =%x\n", lcd_id);	
	printf("lcd_id_pin1 anan =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	

	return (lcd_id  == 0)?1:0;
#else
	return 1;
#endif
}


LCM_DRIVER ili9881c_hd720_dsi_vdo_hsd_wss_mvg6001_mx2132e_ber_s6002dm_lcm_drv = 
{
    .name			= "ili9881c_hd720_dsi_vdo_hsd_wss_mvg6001_mx2132e_ber_s6002dm",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
    };
