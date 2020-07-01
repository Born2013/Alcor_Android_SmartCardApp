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

#define FRAME_WIDTH  (540)
#define FRAME_HEIGHT (960)

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

		pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x05);
		//pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		//pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x01);
	}	
	else
	{
		pmic_set_register_value(PMIC_RG_VGP1_EN,0);
		//pmic_set_register_value(PMIC_RG_VCAMA_EN,0);
	}

	MDELAY(20);

}

struct LCM_setting_table {
    unsigned cmd;
    unsigned char count;
    unsigned char para_list[128];
};


static struct LCM_setting_table lcm_initialization_setting[] = {
{0xFF, 3, {0x98,0x85,0x01}},
{0x01, 1, {0x00}},     
{0x02, 1, {0x00}},     
{0x03, 1, {0x53}},     
{0x04, 1, {0x13}},     
{0x05, 1, {0x53}},     
{0x06, 1, {0x06}},     
{0x07, 1, {0x07}},     
{0x08, 1, {0x07}},     
{0x09, 1, {0x00}},     
{0x0a, 1, {0x00}},     
{0x0b, 1, {0x00}},     
{0x0c, 1, {0x37}},     
{0x0d, 1, {0x37}},     
{0x0e, 1, {0x37}},     
{0x0f, 1, {0x00}},     
{0x10, 1, {0x00}},     
{0x11, 1, {0x00}},     
{0x12, 1, {0x00}},     
{0x13, 1, {0x12}},   
{0x14, 1, {0x00}},     
{0x15, 1, {0x00}},     
{0x16, 1, {0x00}},     
{0x17, 1, {0x00}},     
{0x18, 1, {0x00}},     
{0x19, 1, {0x00}},     
{0x1a, 1, {0x00}},     
{0x1b, 1, {0x00}},     
{0x1c, 1, {0x00}},     
{0x1d, 1, {0x00}},     
{0x1e, 1, {0x48}},     
{0x1f, 1, {0x88}},     
{0x20, 1, {0x03}},     
{0x21, 1, {0x03}},     
{0x22, 1, {0x00}},     
{0x23, 1, {0x00}},     
{0x24, 1, {0x00}},     
{0x25, 1, {0x00}},     
{0x26, 1, {0x00}},     
{0x27, 1, {0x00}},     
{0x28, 1, {0x33}},     
{0x29, 1, {0x02}},     
{0x2a, 1, {0x00}},     
{0x2b, 1, {0x00}},     
{0x2c, 1, {0x00}},     
{0x2d, 1, {0x00}},     
{0x2e, 1, {0x00}},     
{0x2f, 1, {0x00}},     
{0x30, 1, {0x00}},     
{0x31, 1, {0x00}},     
{0x32, 1, {0x00}},     
{0x33, 1, {0x00}},     
{0x34, 1, {0x00}},     
{0x35, 1, {0x00}},     
{0x36, 1, {0x00}},     
{0x37, 1, {0x00}},     
{0x38, 1, {0x00}},     
{0x39, 1, {0x1f}},     
{0x3a, 1, {0x22}},     
{0x3b, 1, {0xc0}},     
{0x3c, 1, {0x00}},     
{0x3d, 1, {0x00}},     
{0x3e, 1, {0x00}},     
{0x3f, 1, {0x00}},     
{0x40, 1, {0x00}},     
{0x41, 1, {0xa0}},     
{0x42, 1, {0x44}},     
{0x43, 1, {0x0c}},     
{0x44, 1, {0x31}},     
{0x45, 1, {0xa8}},     
{0x46, 1, {0x00}},     
{0x47, 1, {0x00}},
                                            
// ==== GOUT_BW_L[3:0] ====
{0x4a, 1, {0x72}},     
{0x4b, 1, {0xbf}},     
{0x4c, 1, {0x62}},     
{0x4d, 1, {0x7a}},     
{0x4e, 1, {0x8b}},     
{0x4f, 1, {0x94}},     
{0x50, 1, {0x25}},     
{0x51, 1, {0x62}},     
{0x52, 1, {0xae}},     
{0x53, 1, {0x01}},     
                                            
// ==== GOUT_BW_R[3:0] ====
{0x54, 1, {0x62}},     
{0x55, 1, {0xae}},     
{0x56, 1, {0x62}},     
{0x57, 1, {0x7a}},     
{0x58, 1, {0x8b}},     
{0x59, 1, {0x94}},     
{0x5a, 1, {0x25}},     
{0x5b, 1, {0x72}},     
{0x5c, 1, {0xbf}},     
{0x5d, 1, {0x01}},     
{0x5e, 1, {0xee}},     
                                            
// ==== GOUT_BW_L ====
{0x5f, 1, {0x02}},
{0x60, 1, {0x07}},
{0x61, 1, {0x0f}},
{0x62, 1, {0x0b}},
{0x63, 1, {0x02}},
{0x64, 1, {0x16}},
{0x65, 1, {0x1a}},
{0x66, 1, {0x17}},
{0x67, 1, {0x1b}},
{0x68, 1, {0x18}},
{0x69, 1, {0x14}},
{0x6a, 1, {0x19}},
{0x6b, 1, {0x15}},
{0x6c, 1, {0x02}},
{0x6d, 1, {0x02}},
{0x6e, 1, {0x06}},
{0x6f, 1, {0x0e}},
{0x70, 1, {0x0a}},
{0x71, 1, {0x01}},
{0x72, 1, {0x00}},
                                            
// ==== GOUT_BW_R ====
{0x73, 1, {0x02}},
{0x74, 1, {0x06}},
{0x75, 1, {0x0e}},
{0x76, 1, {0x0a}},
{0x77, 1, {0x02}},
{0x78, 1, {0x16}},
{0x79, 1, {0x1a}},
{0x7a, 1, {0x17}},
{0x7b, 1, {0x1b}},
{0x7c, 1, {0x18}},
{0x7d, 1, {0x14}},
{0x7e, 1, {0x19}},
{0x7f, 1, {0x15}},
{0x80, 1, {0x02}},
{0x81, 1, {0x02}},
{0x82, 1, {0x07}},
{0x83, 1, {0x0f}},
{0x84, 1, {0x0b}},
{0x85, 1, {0x01}},
{0x86, 1, {0x00}},
                                               
{0xFF, 3, {0x98,0x85,0x02}},
{0x05, 1, {0x44}},     
{0x42, 1, {0x3B}},            //SDT=3.2us (tsVD and thVD>3us)
{0x4D, 1, {0x80}},   
 
//source正負極性waveform相同
{0xFF, 3, {0x98,0x85,0x05}},
{0x1C, 1, {0x00}},

{0xFF, 3, {0x98,0x85,0x05}},
{0x63, 1, {0xFF}},               //enable VGH_REG, VGL_REG
{0x04, 1, {0x51}},  	        //Vcom = -1.09V
{0x30, 1, {0x01}},		//VGL Pump x3 single Mode

{0x3B, 1, {0x12}},		//VGH CLAMP = 10V (12 = 10.8V)
{0x3D, 1, {0x2F}},		//VGL CLAMP = -14V (2F = -14.6V)
{0x3F, 1, {0x0E}},               //VGHO = 10V
{0x40, 1, {0x2C}},               //VGLO = -14V

{0xFF, 3, {0x98,0x85,0x06}},
{0xA3, 1, {0x00}},              //disable AVDDR, AVEER
{0xD2, 1, {0x1A}},              //2lane
{0xD3, 1, {0x05}},              //2lane

{0xFF, 3, {0x98,0x85,0x07}},
{0xE0, 1, {0x01}},              //QHD scale

//============Gamma START=============//
{0xFF, 3, {0x98,0x85,0x08}},
{0x80, 1, {0x00}},	
{0x81, 1, {0x5B}},	
{0x82, 1, {0x00}},	
{0x83, 1, {0x76}},	
{0x84, 1, {0x00}},	
{0x85, 1, {0x9B}},	
{0x86, 1, {0x00}},	
{0x87, 1, {0xB5}},	
{0x88, 1, {0x00}},	
{0x89, 1, {0xCB}},	
{0x8A, 1, {0x00}},	
{0x8B, 1, {0xDD}},	
{0x8C, 1, {0x00}},	
{0x8D, 1, {0xED}},	
{0x8E, 1, {0x00}},	
{0x8F, 1, {0xFC}},	
{0x90, 1, {0x01}},	
{0x91, 1, {0x09}},	
{0x92, 1, {0x01}},	
{0x93, 1, {0x35}},	
{0x94, 1, {0x01}},	
{0x95, 1, {0x58}},	
{0x96, 1, {0x01}},	
{0x97, 1, {0x90}},	
{0x98, 1, {0x01}},	
{0x99, 1, {0xBC}},	
{0x9A, 1, {0x02}},	
{0x9B, 1, {0x02}},	
{0x9C, 1, {0x02}},	
{0x9D, 1, {0x3A}},	
{0x9E, 1, {0x02}},	
{0x9F, 1, {0x3C}},	
{0xA0, 1, {0x02}},	
{0xA1, 1, {0x6E}},	
{0xA2, 1, {0x02}},	
{0xA3, 1, {0xA3}},	
{0xA4, 1, {0x02}},	
{0xA5, 1, {0xC6}},	
{0xA6, 1, {0x02}},	
{0xA7, 1, {0xF5}},	
{0xA8, 1, {0x03}},	
{0xA9, 1, {0x16}},	
{0xAA, 1, {0x03}},	
{0xAB, 1, {0x42}},	
{0xAC, 1, {0x03}},	
{0xAD, 1, {0x4F}},	
{0xAE, 1, {0x03}},	
{0xAF, 1, {0x5F}},	
{0xB0, 1, {0x03}},	
{0xB1, 1, {0x70}},	
{0xB2, 1, {0x03}},	
{0xB3, 1, {0x84}},	
{0xB4, 1, {0x03}},	
{0xB5, 1, {0x9B}},	
{0xB6, 1, {0x03}},	
{0xB7, 1, {0xB8}},	
{0xB8, 1, {0x03}},	
{0xB9, 1, {0xE1}},	
{0xBA, 1, {0x03}},	
{0xBB, 1, {0xE6}},	

//=========== Neg Registe==========//
{0xFF, 3, {0x98,0x85,0x09}},
{0x80, 1, {0x00}},	
{0x81, 1, {0x5B}},	
{0x82, 1, {0x00}},	
{0x83, 1, {0x76}},	
{0x84, 1, {0x00}},	
{0x85, 1, {0x9B}},	
{0x86, 1, {0x00}},	
{0x87, 1, {0xB5}},	
{0x88, 1, {0x00}},	
{0x89, 1, {0xCB}},	
{0x8A, 1, {0x00}},	
{0x8B, 1, {0xDD}},	
{0x8C, 1, {0x00}},	
{0x8D, 1, {0xED}},	
{0x8E, 1, {0x00}},	
{0x8F, 1, {0xFC}},	
{0x90, 1, {0x01}},	
{0x91, 1, {0x09}},	
{0x92, 1, {0x01}},	
{0x93, 1, {0x35}},	
{0x94, 1, {0x01}},	
{0x95, 1, {0x58}},	
{0x96, 1, {0x01}},	
{0x97, 1, {0x90}},	
{0x98, 1, {0x01}},	
{0x99, 1, {0xBC}},	
{0x9A, 1, {0x02}},	
{0x9B, 1, {0x02}},	
{0x9C, 1, {0x02}},	
{0x9D, 1, {0x3A}},	
{0x9E, 1, {0x02}},	
{0x9F, 1, {0x3C}},	
{0xA0, 1, {0x02}},	
{0xA1, 1, {0x6E}},	
{0xA2, 1, {0x02}},	
{0xA3, 1, {0xA3}},	
{0xA4, 1, {0x02}},	
{0xA5, 1, {0xC6}},	
{0xA6, 1, {0x02}},	
{0xA7, 1, {0xF5}},	
{0xA8, 1, {0x03}},	
{0xA9, 1, {0x16}},	
{0xAA, 1, {0x03}},	
{0xAB, 1, {0x42}},	
{0xAC, 1, {0x03}},	
{0xAD, 1, {0x4F}},	
{0xAE, 1, {0x03}},	
{0xAF, 1, {0x5F}},	
{0xB0, 1, {0x03}},	
{0xB1, 1, {0x70}},	
{0xB2, 1, {0x03}},	
{0xB3, 1, {0x84}},	
{0xB4, 1, {0x03}},	
{0xB5, 1, {0x9B}},	
{0xB6, 1, {0x03}},	
{0xB7, 1, {0xB8}},	
{0xB8, 1, {0x03}},	
{0xB9, 1, {0xE1}},	
{0xBA, 1, {0x03}},	
{0xBB, 1, {0xE6}},	

{0xFF,3,{0x98,0x85,0x05}},
{0x1a,1,{0x50}},
{0x22,1,{0x66}},
{0x64,1,{0xCF}},
{0xFF,3,{0x98,0x85,0x02}},
{0x01,1,{0x34}},

//============Gamma END=============//

{0xFF, 3, {0x98,0x85,0x00}},
{0x11, 1, {0x00}},
{REGFLAG_DELAY,120,{}},
{0xFF,3,{0x98,0x85,0x05}},
{0x3B,1,{0x12}},         //VGH CLAMP = 10V (12 = 10.8V)
{0x3D,1,{0x2F}},         //VGL CLAMP = -14V (2F = -14.6V)
{0x3F,1,{0x0E}},               //VGHO = 10V
{0x40,1,{0x2C}},               //VGLO = -14V

{0xFF,3,{0x98,0x85,0x00}},
{0x29, 1, {0x00}},
{0x35, 1, {0x00}},
{REGFLAG_DELAY,20,{}},	


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
		params->dbi.te_mode 			= LCM_DBI_TE_MODE_DISABLED;  //LCM_DBI_TE_MODE_VSYNC_ONLY;
		params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

		params->dsi.mode   = BURST_VDO_MODE;

	
		// DSI
		/* Command mode setting */
		params->dsi.LANE_NUM				    = LCM_TWO_LANE;
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


		params->dsi.PLL_CLOCK = 250;//230//220//240//450; //this value must be in MTK suggested table
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
    //MDELAY(100);
    SET_RESET_PIN(1);
	MDELAY(20);
    SET_RESET_PIN(0);
	MDELAY(20);
    SET_RESET_PIN(1);
	MDELAY(50);

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
//	push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);
}




static unsigned int lcm_compare_id(void)
{
#ifdef BUILD_LK
	unsigned int lcd_id1 = 0;

	lcd_power_en(1);
	MDELAY(50);
   	lcd_id1 = (mt_get_gpio_in(GPIO_LCD_ID1_PIN));
   	//lcd_id2 = (mt_get_gpio_in(GPIO_LCD_ID2_PIN));

	printf("lcd_id1 anan =%x\n", lcd_id1);	
	//printf("lcd_id2 anan =%x\n", lcd_id2);	
	printf("lcd_id_pin1 anan =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));
	//printf("lcd_id_pin2 anan =%x\n", mt_get_gpio_in(GPIO_LCD_ID2_PIN));	

	return (lcd_id1  == 1)?1:0;
#else
	return 1;
#endif
}


LCM_DRIVER ili9885_qhd_dsi_vdo_xp_dt_dt552xxuv0_mx2120e_hds_s702_lcm_drv = 
{
    .name			= "ili9885_qhd_dsi_vdo_xp_dt_dt552xxuv0_mx2120e_hds_s702",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
};
