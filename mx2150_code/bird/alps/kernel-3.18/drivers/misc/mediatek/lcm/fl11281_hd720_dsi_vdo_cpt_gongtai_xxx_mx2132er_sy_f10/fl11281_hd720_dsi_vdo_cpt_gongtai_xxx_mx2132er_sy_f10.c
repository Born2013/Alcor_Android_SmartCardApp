#ifndef BUILD_LK
#include <linux/string.h>
#endif
#include "lcm_drv.h"
#ifdef BUILD_LK
	#include <platform/upmu_common.h>
	#include <platform/upmu_hw.h>
	#include <platform/mt_gpio.h>
	#include <platform/mt_i2c.h> 
	#include <platform/mt_pmic.h>
	#include <string.h>
#else
	#include <mt-plat/upmu_common.h>
	#include <mt-plat/mt_gpio.h>
	#include <linux/gpio.h>
	#include <linux/kernel.h>
#endif

// ---------------------------------------------------------------------------
//  Local Constants
// ---------------------------------------------------------------------------

#define FRAME_WIDTH  (720)
#define FRAME_HEIGHT (1280)

#define REGFLAG_DELAY             							0XFFE
#define REGFLAG_END_OF_TABLE      							0xFFF   // END OF REGISTERS MARKER

#define LCM_DSI_CMD_MODE									0

// ---------------------------------------------------------------------------
//  Local Variables
// ---------------------------------------------------------------------------
//static unsigned int lcm_esd_test = FALSE; ///only for ESD test
//static unsigned int lcm_check_status(void);
static LCM_UTIL_FUNCS lcm_util = {0};

#define SET_RESET_PIN(v)    (lcm_util.set_reset_pin((v)))


#define UDELAY(n) (lcm_util.udelay(n))
#define MDELAY(n) (lcm_util.mdelay(n))

#define LCM_RM68200_ID 		(0x6820)

// ---------------------------------------------------------------------------
//  Local Functions
// ---------------------------------------------------------------------------

#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)	        lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update)
#define dsi_set_cmdq(pdata, queue_size, force_update)		lcm_util.dsi_set_cmdq(pdata, queue_size, force_update)
#define wrtie_cmd(cmd)										lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)					lcm_util.dsi_write_regs(addr, pdata, byte_nums)
#define read_reg											lcm_util.dsi_read_reg()
#define read_reg_v2(cmd, buffer, buffer_size)   			lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)    
       
static void lcd_power_en(unsigned char enabled)
{
	if (enabled)
	{

		//pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		//pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x05);
		pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x01);
	}	
	else
	{
		//pmic_set_register_value(PMIC_RG_VGP1_EN,0);
		pmic_set_register_value(PMIC_RG_VCAMA_EN,0);
	}
	MDELAY(20);
}


struct LCM_setting_table {
    unsigned cmd;
    unsigned char count;
    unsigned char para_list[64];
};


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
	{REGFLAG_DELAY, 120, {}},
    // Sleep Mode On
	{0x10, 1, {0x00}},
	{REGFLAG_DELAY, 120, {}},

	{REGFLAG_END_OF_TABLE, 0x00, {}}
};



static struct LCM_setting_table lcm_initialization_setting[] = {

{0xB9,3,{0xF1,0x12,0x81}},

{0xBA, 27, {0x33, 0x81, 0x05, 0xF9, 0x0E, 0x0E, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x44, 0x25, 0x00, 0x91, 0x0A, 0x00, 0x00, 0x02, 0x4F, 0x11, 0x00, 0x00, 0x37}}, 

{0xB8, 1, {0xA5}}, 



{0xB3,17,{0x02,0x00,0x06,0x06,0x07,0x0B,0x1E,0x1E,0x00,0x00,0x00,0x03,0xFF,0x00,0x00,0x00,0x00}},

{0xC0, 9, {0x73, 0x73, 0x50, 0x50, 0x00, 0x00, 0x08, 0x70, 0x00}}, 

{0xBC,1,{0x46}},

{0xCC,1,{0x0B}},

{0xB4, 1, {0x80}}, 


{0xB2, 3, {0xC8, 0x12, 0x00}}, 

{0xBF, 2, {0x02, 0x11}}, 

{0xE3,10,{0x03,0x03,0x03,0x03,0x03,0x03,0x03,0x03,0x00,0x14}},

{0xB1, 10, {0x21, 0x56, 0xE3, 0x2C, 0x2F, 0x33, 0x77, 0x01, 0xDB, 0x0C}}, 

{0xB5, 2, {0x09, 0x09}}, 

{0xB6, 2, {0x86, 0x86}}, 

{0xE9, 63, {0x00, 0x00, 0x0D, 0x05, 0x13, 0x05, 0xA0, 0x12, 0x31, 0x23, 0x38, 0x11, 0x05, 0xA0, 0x37, 0x0A, 0x08, 0x00, 0x01, 0x00, 0x00, 0x00, 0x08, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,  0x24,  0x68,  0x88,  0x88,  0x88,  0x88,  0x88,  0x89,  0x84,  0x88, 0x11,  0x35,  0x78,  0x88,  0x88,  0x88,  0x88,  0x88,  0x89,  0x85,  0x88, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}}, 

{0xEA, 48, {0x02, 0x1A, 0x00, 0x00, 0x57, 0x53, 0x18, 0x88, 0x88, 0x88, 0x88, 0x88, 0x88, 0x91, 0x88, 0x46, 0x42, 0x08, 0x88, 0x88, 0x88, 0x88, 0x88, 0x88, 0x90, 0x88, 0x00, 0x0F, 0x00, 0xFF, 0x00, 0x04, 0x00, 0x00, 0x3C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}}, 

{0xE0, 34, {0x00, 0x03, 0x04, 0x2E, 0x3A, 0x3F, 0x35, 0x3B, 0x05, 0x08, 0x0D, 0x11, 0x12, 0x10, 0x12, 0x11, 0x15, 0x00, 0x03, 0x04, 0x2E, 0x3A, 0x3F, 0x35, 0x3B, 0x05, 0x08, 0x0D, 0x11, 0x12, 0x10, 0x12, 0x11, 0x15}}, 

{0x11,1,{0x00}},	//sleep out
{REGFLAG_DELAY, 120, {}},
{0x29,1,{0x00}},	//display on
{REGFLAG_DELAY, 20, {}},


{REGFLAG_END_OF_TABLE,0x00,{}}
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
//                dsi_set_cmdq_dcs(cmd, table[i].count, table[i].para_list, force_update);
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

		params->dsi.mode   = BURST_VDO_MODE;

	
		// DSI
		/* Command mode setting */
		params->dsi.LANE_NUM				    = LCM_FOUR_LANE;
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

		
		params->dsi.vertical_sync_active				= 5;
		params->dsi.vertical_backporch				= 15;
		params->dsi.vertical_frontporch				= 30; //20;	//16;
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		params->dsi.horizontal_sync_active			= 20;//20
		params->dsi.horizontal_backporch				= 30;//30
		params->dsi.horizontal_frontporch				= 30;//30
		params->dsi.horizontal_active_pixel 			= FRAME_WIDTH;

/*
		// Bit rate calculation
		params->dsi.pll_div1=1;		// div1=0,1,2,3;div1_real=1,2,4,4
		params->dsi.pll_div2=0;		// div2=0,1,2,3;div2_real=1,2,4,4
		params->dsi.fbk_div =18;		// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)		
*/
		params->dsi.PLL_CLOCK = 220;//240; //this value must be in MTK suggested table

#if 0
		params->dsi.ssc_disable					= 0;
		//params->dsi.ssc_range = 3;
	//params->dsi.clk_lp_per_line_enable = 0;

#endif
#if 1
		params->dsi.esd_check_enable 			= 1;
		params->dsi.customization_esd_check_enable 	= 1;
		params->dsi.lcm_esd_check_table[0].cmd 		= 0x0A;
		params->dsi.lcm_esd_check_table[0].count 	= 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] =0x9C;
		params->dsi.lcm_esd_check_table[1].cmd 		= 0x09;
		params->dsi.lcm_esd_check_table[1].count 	= 1;
		params->dsi.lcm_esd_check_table[1].para_list[0] =0x80;
 #endif
}


static void lcm_init(void)
{
	lcd_power_en(1);
	//MDELAY(50);
	
	SET_RESET_PIN(1);
	MDELAY(20);
	SET_RESET_PIN(0);
	MDELAY(30);
	SET_RESET_PIN(1);
	MDELAY(150);

    push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);


}



static void lcm_suspend(void)
{

        push_table(lcm_sleep_in_setting, sizeof(lcm_sleep_in_setting) / sizeof(struct LCM_setting_table), 1);
		
	SET_RESET_PIN(0);
	MDELAY(10);
	SET_RESET_PIN(1);
  	MDELAY(120);

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
static void lcm_resume(void)
{

	lcm_init();
//	push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);
    lcm_compare_id();
}

LCM_DRIVER fl11281_hd720_dsi_vdo_cpt_gongtai_xxx_mx2132er_sy_f10_lcm_drv = 
{
    .name			= "fl11281_hd720_dsi_vdo_cpt_gongtai_xxx_mx2132er_sy_f10",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
};

