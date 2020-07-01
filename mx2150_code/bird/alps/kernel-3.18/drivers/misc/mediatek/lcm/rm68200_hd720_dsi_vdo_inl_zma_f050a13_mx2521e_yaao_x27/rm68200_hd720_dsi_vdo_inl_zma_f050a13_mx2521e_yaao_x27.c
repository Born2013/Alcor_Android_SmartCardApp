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

#define REGFLAG_DELAY             							0XF1
#define REGFLAG_END_OF_TABLE      							0x100   // END OF REGISTERS MARKER

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

		pmic_set_register_value(PMIC_RG_VGP1_EN,1);//2.8v
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
    unsigned char para_list[64];
};


static struct LCM_setting_table lcm_initialization_setting[] = {
	
{0xFE, 1,{0x01}},

{0x00, 1,{0x0A}},

{0x01, 1,{0x08}},

{0x0E, 1,{0x27}},

{0x2B, 1,{0xC5}},

{0x16, 1,{0x52}},

{0x27, 1,{0x0A}},

{0x29, 1,{0x0A}},

{0x2F, 1,{0x6A}},

{0x34, 1,{0x55}},

{0x1B, 1,{0x00}},

{0x12, 1,{0x08}},

{0x1A, 1,{0x06}},

{0x46, 1,{0x75}},

{0x52, 1,{0x95}},

{0x53, 1,{0x00}},

{0x54, 1,{0x95}},

{0x55, 1,{0x00}},

{0x5F, 1,{0x12}},//3//  3LAN


//{REGFLAG_DELAY, 200, {}},

{0xFE, 1,{0x03}},

{0x00, 1,{0x05}},

{0x01, 1,{0x16}},

{0x02, 1,{0x01}},

{0x03, 1,{0x04}},

{0x04, 1,{0x00}},

{0x05, 1,{0x00}},

{0x06, 1,{0x50}},

{0x07, 1,{0x05}},

{0x08, 1,{0x16}},

{0x09, 1,{0x01}},

{0x0A, 1,{0x08}},

{0x0B, 1,{0x00}},

{0x0C, 1,{0x00}},

{0x0D, 1,{0x50}},

{0x0E, 1,{0x03}},

{0x0F, 1,{0x04}},

{0x10, 1,{0x05}},

{0x11, 1,{0x06}},

{0x12, 1,{0x00}},

{0x13, 1,{0x40}},

{0x14, 1,{0x00}},

{0x15, 1,{0xC5}},

{0x16, 1,{0x0C}},

{0x17, 1,{0x07}},

{0x18, 1,{0x08}},

{0x19, 1,{0x09}},

{0x1A, 1,{0x0A}},

{0x1B, 1,{0x00}},

{0x1C, 1,{0x40}},

{0x1D, 1,{0x00}},

{0x1E, 1,{0x85}},

{0x1F, 1,{0x0C}},

{0x20, 1,{0x00}},

{0x21, 1,{0x00}},

{0x22, 1,{0x03}},

{0x23, 1,{0x0A}},

{0x24, 1,{0x0C}},

{0x25, 1,{0x2D}},

{0x26, 1,{0x00}},

{0x27, 1,{0x0E}},

{0x28, 1,{0x10}},

{0x29, 1,{0x2D}},

{0x2A, 1,{0x0B}},

{0x2B, 1,{0x0C}},

{0x2D, 1,{0x0D}},

{0x2F, 1,{0x0E}},

{0x30, 1,{0x00}},

{0x31, 1,{0x40}},

{0x32, 1,{0x05}},

{0x33, 1,{0x0C}},

{0x34, 1,{0x40}},

{0x35, 1,{0x00}},

{0x36, 1,{0x00}},

{0x37, 1,{0x00}},

{0x38, 1,{0x00}},

{0x39, 1,{0x00}},

{0x3A, 1,{0x00}},

{0x3B, 1,{0x00}},

{0x3D, 1,{0x00}},

{0x3F, 1,{0x00}},

{0x40, 1,{0x00}},

{0x41, 1,{0x00}},

{0x42, 1,{0x00}},

{0x43, 1,{0x00}},

{0x44, 1,{0x00}},

{0x45, 1,{0x00}},

{0x46, 1,{0x00}},

{0x47, 1,{0x00}},

{0x48, 1,{0x00}},

{0x49, 1,{0x00}},

{0x4A, 1,{0x00}},

{0x4B, 1,{0x00}},

{0x4C, 1,{0x00}},

{0x4D, 1,{0x00}},

{0x4E, 1,{0x00}},

{0x4F, 1,{0x00}},

{0x50, 1,{0x00}},

{0x51, 1,{0x00}},

{0x52, 1,{0x00}},

{0x53, 1,{0x00}},

{0x54, 1,{0x00}},

{0x55, 1,{0x00}},

{0x56, 1,{0x00}},

{0x58, 1,{0x00}},

{0x59, 1,{0x00}},

{0x5A, 1,{0x00}},

{0x5B, 1,{0x00}},

{0x5C, 1,{0x00}},

{0x5D, 1,{0x00}},

{0x5E, 1,{0x00}},

{0x5F, 1,{0x00}},

{0x60, 1,{0x0B}},

{0x61, 1,{0x00}},

{0x62, 1,{0x80}},

{0x63, 1,{0x12}},

{0x64, 1,{0x14}},

{0x65, 1,{0x00}},

{0x66, 1,{0x00}},

{0x67, 1,{0x55}},

{0x68, 1,{0x00}},

{0x69, 1,{0x00}},

{0x6A, 1,{0x00}},

{0x6B, 1,{0x00}},

{0x6C, 1,{0x00}},

{0x6D, 1,{0xBC}},

{0x6E, 1,{0x00}},

{0x6F, 1,{0x00}},

{0x70, 1,{0x00}},

{0x71, 1,{0x80}},

{0x72, 1,{0x00}},

{0x73, 1,{0x00}},

{0x74, 1,{0x01}},

{0x75, 1,{0x01}},

{0x76, 1,{0x01}},

{0x77, 1,{0x01}},

{0x78, 1,{0x01}},

{0x79, 1,{0x01}},

{0x7A, 1,{0x00}},

{0x7B, 1,{0x00}},

{0x7C, 1,{0x00}},

{0x7D, 1,{0x00}},

{0x7E, 1,{0x18}},

{0x7F, 1,{0x19}},

{0x80, 1,{0x09}},

{0x81, 1,{0x0B}},

{0x82, 1,{0x0D}},

{0x83, 1,{0x0F}},

{0x84, 1,{0x11}},

{0x85, 1,{0x13}},

{0x86, 1,{0x03}},

{0x87, 1,{0x3F}},

{0x88, 1,{0x07}},

{0x89, 1,{0x3F}},

{0x8A, 1,{0x1C}},

{0x8B, 1,{0x3F}},

{0x8C, 1,{0x3F}},

{0x8D, 1,{0x1C}},

{0x8E, 1,{0x1C}},

{0x8F, 1,{0x1C}},

{0x90, 1,{0x3F}},

{0x91, 1,{0x01}},

{0x92, 1,{0x3F}},

{0x93, 1,{0x3F}},

{0x94, 1,{0x3F}},

{0x95, 1,{0x3F}},

{0x96, 1,{0x00}},

{0x97, 1,{0x3F}},

{0x98, 1,{0x1C}},

{0x99, 1,{0x1C}},

{0x9A, 1,{0x1C}},

{0x9B, 1,{0x3F}},

{0x9C, 1,{0x3F}},

{0x9D, 1,{0x1C}},

{0x9E, 1,{0x3F}},

{0x9F, 1,{0x06}},

{0xA0, 1,{0x3F}},

{0xA2, 1,{0x02}},

{0xA3, 1,{0x12}},

{0xA4, 1,{0x10}},

{0xA5, 1,{0x0E}},

{0xA6, 1,{0x0C}},

{0xA7, 1,{0x0A}},

{0xA9, 1,{0x08}},

{0xAA, 1,{0x19}},

{0xAB, 1,{0x18}},

{0xAC, 1,{0x18}},

{0xAD, 1,{0x19}},

{0xAE, 1,{0x12}},

{0xAF, 1,{0x10}},

{0xB0, 1,{0x0E}},

{0xB1, 1,{0x0C}},

{0xB2, 1,{0x0A}},

{0xB3, 1,{0x08}},

{0xB4, 1,{0x00}},

{0xB5, 1,{0x3F}},

{0xB6, 1,{0x06}},

{0xB7, 1,{0x3F}},

{0xB8, 1,{0x1C}},

{0xB9, 1,{0x3F}},

{0xBA, 1,{0x3F}},

{0xBB, 1,{0x1C}},

{0xBC, 1,{0x1C}},

{0xBD, 1,{0x1C}},

{0xBE, 1,{0x3F}},

{0xBF, 1,{0x02}},

{0xC0, 1,{0x3F}},

{0xC1, 1,{0x3F}},

{0xC2, 1,{0x3F}},

{0xC3, 1,{0x3F}},

{0xC4, 1,{0x03}},

{0xC5, 1,{0x3F}},

{0xC6, 1,{0x1C}},

{0xC7, 1,{0x1C}},

{0xC8, 1,{0x1C}},

{0xC9, 1,{0x3F}},

{0xCA, 1,{0x3F}},

{0xCB, 1,{0x1C}},

{0xCC, 1,{0x3F}},

{0xCD, 1,{0x07}},

{0xCE, 1,{0x3F}},

{0xCF, 1,{0x01}},

{0xD0, 1,{0x09}},

{0xD1, 1,{0x0B}},

{0xD2, 1,{0x0D}},

{0xD3, 1,{0x0F}},

{0xD4, 1,{0x11}},

{0xD5, 1,{0x13}},

{0xD6, 1,{0x19}},

{0xD7, 1,{0x18}},

{0xDC, 1, {0x02}},
{0xDE, 1, {0x08}},



{0xDF, 1,{0x00}},



//{REGFLAG_DELAY, 200, {}},

{0xFE, 1,{0x0E}},
{0x01, 1,{0x75}},

{0x54, 1,{0x01}},


{0xFE, 1,{0x04}},

{0x60, 1,{0x00}},

{0x61, 1,{0x10}},

{0x62, 1,{0x16}},

{0x63, 1,{0x0F}},

{0x64, 1,{0x06}},

{0x65, 1,{0x13}},

{0x66, 1,{0x0F}},

{0x67, 1,{0x0B}},

{0x68, 1,{0x16}},

{0x69, 1,{0x0C}},

{0x6A, 1,{0x10}},

{0x6B, 1,{0x07}},

{0x6C, 1,{0x0F}},

{0x6D, 1,{0x15}},

{0x6E, 1,{0x0F}},

{0x6F, 1,{0x00}},

{0x70, 1,{0x00}},

{0x71, 1,{0x10}},

{0x72, 1,{0x16}},

{0x73, 1,{0x0F}},

{0x74, 1,{0x06}},

{0x75, 1,{0x13}},

{0x76, 1,{0x0F}},

{0x77, 1,{0x0B}},

{0x78, 1,{0x16}},

{0x79, 1,{0x0C}},

{0x7A, 1,{0x10}},

{0x7B, 1,{0x07}},

{0x7C, 1,{0x0F}},

{0x7D, 1,{0x15}},

{0x7E, 1,{0x0F}},

{0x7F, 1,{0x00}},

{0xFE, 1,{0x00}},

{0x58, 1,{0x00}},

//{REGFLAG_DELAY, 200, {}},

{0x11, 0,{0x00}},

{REGFLAG_DELAY, 120, {}},

{0x29, 0,{0x00}},

{REGFLAG_DELAY,120,{}},


{REGFLAG_END_OF_TABLE,0x00,{}}
};



static struct LCM_setting_table lcm_sleep_in_setting[] = {
	// Display off sequence
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
		params->dbi.te_mode 				= LCM_DBI_TE_MODE_DISABLED;  //LCM_DBI_TE_MODE_VSYNC_ONLY;
		params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

		params->dsi.mode   = SYNC_PULSE_VDO_MODE;

	
		// DSI
		/* Command mode setting */
		params->dsi.LANE_NUM				    = LCM_THREE_LANE;//LCM_FOUR_LANE;
		//The following defined the fomat for data coming from LCD engine.
		params->dsi.data_format.color_order = LCM_COLOR_ORDER_RGB;
		params->dsi.data_format.trans_seq   = LCM_DSI_TRANS_SEQ_MSB_FIRST;
		params->dsi.data_format.padding     = LCM_DSI_PADDING_ON_LSB;
		params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

		// Highly depends on LCD driver capability.
		// Not support in MT6573
		params->dsi.packet_size=256;

		//params->dsi.intermediat_buffer_num = 0;

		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;
		//params->dsi.word_count=720*3;	

		
		params->dsi.vertical_sync_active				= 2;
		params->dsi.vertical_backporch				    = 14;
		params->dsi.vertical_frontporch 				= 16;
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		params->dsi.horizontal_sync_active				= 8;
		params->dsi.horizontal_backporch				= 64;
		params->dsi.horizontal_frontporch				= 64;
		params->dsi.horizontal_active_pixel 			= FRAME_WIDTH;

/*
		// Bit rate calculation
		params->dsi.pll_div1=1;		// div1=0,1,2,3;div1_real=1,2,4,4
		params->dsi.pll_div2=0;		// div2=0,1,2,3;div2_real=1,2,4,4
		params->dsi.fbk_div =18;		// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)		
*/
	params->dsi.PLL_CLOCK = 267;//240; //this value must be in MTK suggested table
		params->dsi.ssc_disable					= 1;
}


static void lcm_init(void)
{
	lcd_power_en(1);
	MDELAY(50);
	
	SET_RESET_PIN(1);
	MDELAY(10);
	SET_RESET_PIN(0);
	MDELAY(50);
	SET_RESET_PIN(1);
	MDELAY(120);

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

	lcm_init();

    //lcm_initialization_setting[1]. para_list[0]+=2;

	
//	push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);
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

	return (lcd_id  == 3)?1:0;
#else
	return 1;
#endif
}


LCM_DRIVER rm68200_hd720_dsi_vdo_inl_zma_f050a13_mx2521e_yaao_x27_lcm_drv = 
{
    .name			= "rm68200_hd720_dsi_vdo_inl_zma_f050a13_mx2521e_yaao_x27",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
};

