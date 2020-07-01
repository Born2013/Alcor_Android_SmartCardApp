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

		pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x03);
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
	{REGFLAG_DELAY, 20, {}},
    // Sleep Mode On
	{0x10, 1, {0x00}},
	{REGFLAG_DELAY, 120, {}},

	{REGFLAG_END_OF_TABLE, 0x00, {}}
};



static struct LCM_setting_table lcm_initialization_setting[] = {
	{0xFF,3,{0x98,0x81,0x01}},
	{0x44,1,{0x31}},
	{0xFF,3,{0x98,0x81,0x05}},
	{0xB2,1,{0x70}},				
	{0x04,1,{0x28}},
	{0x30,1,{0xf7}},
	{0x29,1,{0x00}},
	{0x2a,1,{0x14}},
	{0x38,1,{0xa8}},//
	{0x1a,1,{0x50}},
	{0x52,1,{0x5f}},
	{0x54,1,{0x28}},//
	{0x55,1,{0x25}},
        {0x26,1,{0x02}},     //Auto 1/2 VCOM-new     0905 add for Ver_AE
        {0x3D,1,{0xA1}},      //default:E1 enable VCI/VSP LVD     0905 add for Ver_AE
        {0x1B,1,{0x01}},     //01, keep LVD function when LVD dis     0905 add for Ver_AE
        {0xFF,3,{0x98,0x81,0x02}}, 
        {0x42,1,{0x2F}},	    //SDT=3us 
        {0x01,1,{0x50}},         //timeout black
        {0x15,1,{0x10}},         //2/3-power mode
        {0x57,1,{0x00}},     
        {0x58,1,{0x16}},     
        {0x59,1,{0x25}},	
        {0x5A,1,{0x13}},     
        {0x5B,1,{0x16}},     
        {0x5C,1,{0x29}},     
        {0x5D,1,{0x1D}},     
        {0x5E,1,{0x1F}},	
        {0x5F,1,{0x85}},     
        {0x60,1,{0x1E}},     
        {0x61,1,{0x2A}},     
        {0x62,1,{0x72}},     
        {0x63,1,{0x19}},	
        {0x64,1,{0x16}},     
        {0x65,1,{0x4A}},     
        {0x66,1,{0x21}},     
        {0x67,1,{0x28}},     
        {0x68,1,{0x4F}},	
        {0x69,1,{0x5D}},     
        {0x6A,1,{0x25}},     
        {0x6B,1,{0x00}},     
        {0x6C,1,{0x16}},     
        {0x6D,1,{0x25}},	
        {0x6E,1,{0x13}},     
        {0x6F,1,{0x16}},     
        {0x70,1,{0x29}},     
        {0x71,1,{0x1D}},     
        {0x72,1,{0x1F}},	
        {0x73,1,{0x85}},     
        {0x74,1,{0x1E}},     
        {0x75,1,{0x2A}},     
        {0x76,1,{0x72}},     
        {0x77,1,{0x19}}, 
        {0x78,1,{0x16}}, 
        {0x79,1,{0x4A}}, 
        {0x7A,1,{0x21}}, 
        {0x7B,1,{0x28}}, 
        {0x7C,1,{0x4F}}, 
        {0x7D,1,{0x5D}}, 
        {0x7E,1,{0x25}}, 
        {0xFF,3,{0x98,0x81,0x06}}, 
        {0x01,1,{0x03}},      //LEDPWM/SDO hi-z  
        {0x2B,1,{0x0A}},     //BGR_PANEL+SS_PANEL 
               
        {0xFF,3,{0x98,0x81,0x00}},
{0x35,1,{0x00}}, 
        {0x11,1,{0x00}},               
{REGFLAG_DELAY,120,{}},     
{0x29,1,{0x00}},  
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

		
		params->dsi.vertical_sync_active					= 2;//8;
		params->dsi.vertical_backporch					= 30;//8;
		params->dsi.vertical_frontporch					= 20;//10;
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		params->dsi.horizontal_sync_active				= 33;//10;
		params->dsi.horizontal_backporch				= 100;//20;
		params->dsi.horizontal_frontporch				= 100;//40;
		params->dsi.horizontal_active_pixel				= FRAME_WIDTH;
		params->dsi.esd_check_enable 			= 1;
		params->dsi.customization_esd_check_enable 	= 1;
		params->dsi.lcm_esd_check_table[0].cmd            = 0x0A;
		params->dsi.lcm_esd_check_table[0].count        = 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] = 0x9C;

		params->dsi.HS_TRAIL = 15;  //new add
		params->dsi.PLL_CLOCK = 230;//240; //this value must be in MTK suggested table

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
	SET_RESET_PIN(1);
	MDELAY(5);	
	SET_RESET_PIN(0);
	MDELAY(20);
	SET_RESET_PIN(1);
  	MDELAY(120);

}


static void lcm_resume(void)
{
#ifndef BUILD_LK
printk("crystal_ili9881p_hd720_dsi_vdo_panda_bx_bx500125_mx2135f_szbd_e5018_\n");
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
   	lcd_id = ((mt_get_gpio_in(GPIO_LCD_ID1_PIN) << 1) & 0x2) | mt_get_gpio_in(GPIO_LCD_ID2_PIN);

	printf("lcd_id anan =%x\n", lcd_id);	
	printf("lcd_id_pin1 anan =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	
	printf("lcd_id_pin2 anan =%x\n", mt_get_gpio_in(GPIO_LCD_ID2_PIN));

	return (lcd_id  == 1)?1:0;
#else
	return 1;
#endif
}


LCM_DRIVER ili9881p_hd720_dsi_vdo_panda_bx_bx500125_mx2135f_szbd_e5018_lcm_drv = 
{
    .name			= "ili9881p_hd720_dsi_vdo_panda_bx_bx500125_mx2135f_szbd_e5018",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
    };
