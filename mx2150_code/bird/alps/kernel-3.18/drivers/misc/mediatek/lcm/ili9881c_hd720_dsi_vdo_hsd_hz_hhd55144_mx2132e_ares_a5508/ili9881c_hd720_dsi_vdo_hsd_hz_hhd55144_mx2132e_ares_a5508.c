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

#define FRAME_WIDTH  										(720)
#define FRAME_HEIGHT 										(1280)

#define REGFLAG_DELAY             							0XFC
#define REGFLAG_END_OF_TABLE      							0xFD   // END OF REGISTERS MARKER

#define LCM_ID	

#define LCM_DSI_CMD_MODE									0

#ifndef TRUE
    #define   TRUE     1
#endif
 
#ifndef FALSE
    #define   FALSE    0
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

		//pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		//pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x01);

		//pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		//pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x07);


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


static struct LCM_setting_table lcm_initialization_setting[] = {

{0xFF,3,{0x98,0x81,0x03}},
{0x01,1,{0x00}},
{0x02,1,{0x00}},
{0x03,1,{0x72}},
{0x04,1,{0x00}},
{0x05,1,{0x00}},
{0x06,1,{0x09}},
{0x07,1,{0x00}},
{0x08,1,{0x00}},
{0x09,1,{0x01}},
{0x0A,1,{0x00}},
{0x0B,1,{0x00}},
{0x0C,1,{0x01}},
{0x0D,1,{0x00}},
{0x0E,1,{0x00}},
{0x0F,1,{0x00}},
{0x10,1,{0x00}},
{0x11,1,{0x00}},
{0x12,1,{0x00}},
{0x13,1,{0x00}},
{0x14,1,{0x00}},
{0x15,1,{0x04}},
{0x16,1,{0x01}},
{0x17,1,{0x01}},
{0x18,1,{0x00}},
{0x19,1,{0x00}},
{0x1A,1,{0x00}},
{0x1B,1,{0x00}},
{0x1C,1,{0x00}},
{0x1D,1,{0x00}},
{0x1E,1,{0x40}},
{0x1F,1,{0x80}},
{0x20,1,{0x05}},
{0x21,1,{0x02}},
{0x22,1,{0x00}},
{0x23,1,{0x00}},
{0x24,1,{0x00}},
{0x25,1,{0x00}},
{0x26,1,{0x00}},
{0x27,1,{0x00}},
{0x28,1,{0x33}},
{0x29,1,{0x02}},
{0x2A,1,{0x00}},
{0x2B,1,{0x00}},
{0x2C,1,{0x00}},
{0x2D,1,{0x00}},
{0x2E,1,{0x00}},
{0x2F,1,{0x00}},
{0x30,1,{0x00}},
{0x31,1,{0x00}},
{0x32,1,{0x00}},
{0x33,1,{0x00}},
{0x34,1,{0x04}},
{0x35,1,{0x00}},
{0x36,1,{0x00}},
{0x37,1,{0x00}},
{0x38,1,{0x3c}},
{0x39,1,{0x00}},
{0x3A,1,{0x40}},
{0x3B,1,{0x40}},
{0x3C,1,{0x00}},
{0x3D,1,{0x00}},
{0x3E,1,{0x00}},
{0x3F,1,{0x00}},
{0x40,1,{0x00}},
{0x41,1,{0x00}},
{0x42,1,{0x00}},
{0x43,1,{0x00}},
{0x44,1,{0x00}},
{0x50,1,{0x01}},
{0x51,1,{0x23}},
{0x52,1,{0x45}},
{0x53,1,{0x67}},
{0x54,1,{0x89}},
{0x55,1,{0xab}},
{0x56,1,{0x01}},
{0x57,1,{0x23}},
{0x58,1,{0x45}},
{0x59,1,{0x67}},
{0x5A,1,{0x89}},
{0x5B,1,{0xab}},
{0x5C,1,{0xcd}},
{0x5D,1,{0xef}},
{0x5E,1,{0x11}},
{0x5F,1,{0x01}},
{0x60,1,{0x00}},
{0x61,1,{0x15}},
{0x62,1,{0x14}},
{0x63,1,{0x0E}},
{0x64,1,{0x0F}},
{0x65,1,{0x0C}},
{0x66,1,{0x0D}},
{0x67,1,{0x06}},
{0x68,1,{0x02}},
{0x69,1,{0x02}},
{0x6A,1,{0x02}},
{0x6B,1,{0x02}},
{0x6C,1,{0x02}},
{0x6D,1,{0x02}},
{0x6E,1,{0x07}},
{0x6F,1,{0x02}},
{0x70,1,{0x02}},
{0x71,1,{0x02}},
{0x72,1,{0x02}},
{0x73,1,{0x02}},
{0x74,1,{0x02}},
{0x75,1,{0x01}},
{0x76,1,{0x00}},
{0x77,1,{0x14}},
{0x78,1,{0x15}},
{0x79,1,{0x0E}},
{0x7A,1,{0x0F}},
{0x7B,1,{0x0C}},
{0x7C,1,{0x0D}},
{0x7D,1,{0x06}},
{0x7E,1,{0x02}},
{0x7F,1,{0x02}},
{0x80,1,{0x02}},
{0x81,1,{0x02}},
{0x82,1,{0x02}},
{0x83,1,{0x02}},
{0x84,1,{0x07}},
{0x85,1,{0x02}},
{0x86,1,{0x02}},
{0x87,1,{0x02}},
{0x88,1,{0x02}},
{0x89,1,{0x02}},
{0x8A,1,{0x02}},
{0xFF,3,{0x98,0x81,0x04}},
{0x6C,1,{0x15}},
{0x6E,1,{0x2A}},
{0x6F,1,{0x33}},//33
{0x3A,1,{0x24}},
{0x33,1,{0x00}},
{0x32,1,{0x05}},
{0x35,1,{0x1f}},

{0x8D,1,{0x1A}},
{0x87,1,{0xBA}},
{0x26,1,{0x76}},
{0xB2,1,{0xD1}},
{0xB5,1,{0x06}},
{0xFF,3,{0x98,0x81,0x01}},
{0x22,1,{0x0A}},
{0x31,1,{0x00}},
{0x53,1,{0x8c}},   //8f   
{0x55,1,{0x8F}},
{0x50,1,{0xAE}},
{0x51,1,{0xAE}},
{0x60,1,{0x28}},
{0x63,1,{0x00}},

{0xA0,1,{0x0f}},
{0xA1,1,{0x21}},
{0xA2,1,{0x2f}},
{0xA3,1,{0x14}},
{0xA4,1,{0x17}},
{0xA5,1,{0x29}},
{0xA6,1,{0x1e}},
{0xA7,1,{0x1f}},
{0xA8,1,{0x89}},
{0xA9,1,{0x1c}},
{0xAA,1,{0x27}},
{0xAB,1,{0x73}},
{0xAC,1,{0x1B}},
{0xAD,1,{0x1B}},
{0xAE,1,{0x4F}},
{0xAF,1,{0x24}},
{0xB0,1,{0x2A}},
{0xB1,1,{0x4B}},
{0xB2,1,{0x58}},
{0xB3,1,{0x3F}},

{0xC0,1,{0x04}},
{0xC1,1,{0x22}},
{0xC2,1,{0x30}},
{0xC3,1,{0x14}},
{0xC4,1,{0x17}},
{0xC5,1,{0x2A}},
{0xC6,1,{0x1E}},
{0xC7,1,{0x1E}},
{0xC8,1,{0x8A}},
{0xC9,1,{0x1B}},
{0xCA,1,{0x28}},
{0xCB,1,{0x74}},
{0xCC,1,{0x1C}},
{0xCD,1,{0x1B}},
{0xCE,1,{0x4F}},
{0xCF,1,{0x24}},
{0xD0,1,{0x2A}},
{0xD1,1,{0x4B}},
{0xD2,1,{0x58}},
{0xD3,1,{0x3F}},
{0xFF,3,{0x98,0x81,0x00}},

//{0x21,01,{0x00}},

{0x11,01,{0x00}},
{REGFLAG_DELAY, 50, {}},  
{0x29,01,{0x00}},	
{REGFLAG_DELAY, 20, {}},  
{REGFLAG_END_OF_TABLE, 0x00, {}} 

};

#if 0
static struct LCM_setting_table lcm_sleep_out_setting[] = {
	// Sleep Out
	{0x11, 1, {0x00}},
	{REGFLAG_DELAY, 120, {}},

	// Display ON
	{0x29, 1, {0x00}},
	{REGFLAG_DELAY, 20, {}},

	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
#endif

static struct LCM_setting_table lcm_deep_sleep_mode_in_setting[] = {
	{0xFF,3,{0x98,0x81,0x01}},
	{0x53, 1, {0x10}},
	{0xb3, 1, {0x3f}},
	{0xd3, 1, {0x3f}},
	
	{0xFF,3,{0x98,0x81,0x04}},
	{0x2d, 1, {0x02}},
	{0x2f, 1, {0x01}},
	{REGFLAG_DELAY, 100, {}},
	{0x2f, 1, {0x00}},
	
	{0xFF,3,{0x98,0x81,0x00}},
	{0x2f, 1, {0x00}},
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
				MDELAY(2);
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
		params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

		params->dsi.mode   = BURST_VDO_MODE;

	
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

		// Video mode setting		
		//params->dsi.intermediat_buffer_num = 2;

		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;

		params->dsi.vertical_sync_active				= 10; 
		params->dsi.vertical_backporch					= 20; 
		params->dsi.vertical_frontporch					= 10; 
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		params->dsi.horizontal_sync_active				= 40;	
		params->dsi.horizontal_backporch				= 100;
		params->dsi.horizontal_frontporch				= 70;
		params->dsi.horizontal_active_pixel				= FRAME_WIDTH;

/*
		// Bit rate calculation
		params->dsi.pll_div1=1;		// div1=0,1,2,3;div1_real=1,2,4,4
		params->dsi.pll_div2=0;		// div2=0,1,2,3;div2_real=1,2,4,4
		params->dsi.fbk_div =18;		// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)		
*/
	params->dsi.PLL_CLOCK = 200;//220//225//240//235//230//204//212//240;   221//this value must be in MTK suggested table
		params->dsi.ssc_disable					= 1;

		params->dsi.HS_TRAIL = 20;
#if 1
		/* ESD or noise interference recovery For video mode LCM only. */ // Send TE packet to LCM in a period of n frames and check the response. 
		params->dsi.lcm_int_te_monitor = FALSE; 
		params->dsi.lcm_int_te_period = 1; // Unit : frames 
 
		// Need longer FP for more opportunity to do int. TE monitor applicably. 
		if(params->dsi.lcm_int_te_monitor) 
			params->dsi.vertical_frontporch *= 2; 
 
		// Monitor external TE (or named VSYNC) from LCM once per 2 sec. (LCM VSYNC must be wired to baseband TE pin.) 
		params->dsi.lcm_ext_te_monitor = FALSE; 
		// Non-continuous clock 
		params->dsi.noncont_clock = TRUE; 
		params->dsi.noncont_clock_period = 2; // Unit : frames


		params->dsi.esd_check_enable 			= 1;
		params->dsi.customization_esd_check_enable 	= 1;
		params->dsi.lcm_esd_check_table[0].cmd 		= 0x0a;//0x0A;
		params->dsi.lcm_esd_check_table[0].count 	= 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] = 0x9c;//0x9C;
#endif
}




static void lcm_init(void)
{
	
	lcd_power_en(1);
    MDELAY(10);
    SET_RESET_PIN(1);
	MDELAY(10);
    SET_RESET_PIN(0);
    MDELAY(10);
    SET_RESET_PIN(1);
    MDELAY(20);

    push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);




}


static void lcm_suspend(void)
{

	push_table(lcm_deep_sleep_mode_in_setting, sizeof(lcm_deep_sleep_mode_in_setting) / sizeof(struct LCM_setting_table), 1);
	SET_RESET_PIN(1);
	SET_RESET_PIN(0);
	MDELAY(10);
	SET_RESET_PIN(1);
}


static void lcm_resume(void)
{



    //push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);


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

	return (lcd_id  == 1)?1:0;
#else
	return 1;
#endif
}

LCM_DRIVER ili9881c_hd720_dsi_vdo_hsd_hz_hhd55144_mx2132e_ares_a5508_lcm_drv = 
{
      .name			= "ili9881c_hd720_dsi_vdo_hsd_hz_hhd55144_mx2132e_ares_a5508",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id    = lcm_compare_id,
};
