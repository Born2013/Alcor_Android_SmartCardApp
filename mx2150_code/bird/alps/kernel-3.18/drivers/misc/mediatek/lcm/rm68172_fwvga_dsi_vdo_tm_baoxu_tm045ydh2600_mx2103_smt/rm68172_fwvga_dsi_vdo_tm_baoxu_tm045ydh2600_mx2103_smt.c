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

#define FRAME_WIDTH  (480)
#define FRAME_HEIGHT (854)

#define REGFLAG_DELAY             							0xAB
#define REGFLAG_END_OF_TABLE      							0xAA   // END OF REGISTERS MARKER


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

#define SET_RESET_PIN(v)    (lcm_util.set_reset_pin((v)))

#define UDELAY(n) (lcm_util.udelay(n))
#define MDELAY(n) (lcm_util.mdelay(n))

#define LCM_ID       (0x7281)

// ---------------------------------------------------------------------------
//  Local Functions
// ---------------------------------------------------------------------------

#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)	lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update)
#define dsi_set_cmdq(pdata, queue_size, force_update)		lcm_util.dsi_set_cmdq(pdata, queue_size, force_update)
#define wrtie_cmd(cmd)									lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)				lcm_util.dsi_write_regs(addr, pdata, byte_nums)
#define read_reg(cmd)											lcm_util.dsi_dcs_read_lcm_reg(cmd)
#define read_reg_v2(cmd, buffer, buffer_size)   				lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)    

static void lcd_power_en(unsigned char enabled)
{
	if (enabled)
	{

		//pmic_set_register_value(PMIC_RG_VGP1_EN,1);//2.8v
		//pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x05);
		pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x01);
		//mt_set_gpio_mode(GPIO_LCD_LDO_EN_PIN , 0);//1.8v
		//mt_set_gpio_dir(GPIO_LCD_LDO_EN_PIN , 1);
		//mt_set_gpio_out(GPIO_LCD_LDO_EN_PIN , 1); 
	}	
	else
	{
	
	//pmic_set_register_value(PMIC_RG_VGP1_EN,0);
		pmic_set_register_value(PMIC_RG_VCAMA_EN,0);
	    //mt_set_gpio_mode(GPIO_LCD_LDO_EN_PIN , 0);
	    //mt_set_gpio_dir(GPIO_LCD_LDO_EN_PIN , 1);
	    //mt_set_gpio_out(GPIO_LCD_LDO_EN_PIN , 0);
	}
	MDELAY(20);

}


struct LCM_setting_table {
    unsigned char cmd;
    unsigned char count;
    unsigned char para_list[64];
};


static struct LCM_setting_table lcm_initialization_setting[] = {

{0xF0, 5,{0x55, 0xAA, 0x52, 0x08, 0x02}},

{0xF6, 2,{0x60, 0x40}},

{0xFE, 4,{0x01, 0x80, 0x09, 0x09}},

{0xF0, 5,{0x55, 0xAA, 0x52, 0x08, 0x01}},

{0xB0, 1,{0x0D}},

{0xB1, 1,{0x0B}},

{0xB6, 1,{0x34}},

{0xB7, 1,{0x44}},

{0xB9, 1,{0x34}},

{0xBA, 1,{0x14}},

{0xBC, 3,{0x00, 0x88, 0x00}},

{0xBD, 3,{0x00, 0x88, 0x00}},

{0xBE, 2,{0x00, 0x5c}},//5d

{0xD1, 52,{0x00, 0x63, 0x00, 0x8A, 0x00, 0xB9, 0x00, 0xD6, 0x00, 0xEF, 0x01, 0x15, 0x01, 0x32, 0x01, 0x61, 0x01, 0x86, 0x01, 0xBF, 0x01, 0xEC, 0x02, 0x31, 0x02, 0x68, 0x02, 0x6A, 0x02, 0x9C, 0x02, 0xCE, 0x02, 0xED, 0x03, 0x13, 0x03, 0x2B, 0x03, 0x49, 0x03, 0x5B, 0x03, 0x6F, 0x03, 0x77, 0x03, 0x81, 0x03, 0x84, 0x03, 0xFF}},

{0xD2, 52,{0x00, 0x63, 0x00, 0x8A, 0x00, 0xB9, 0x00, 0xD6, 0x00, 0xEF, 0x01, 0x15, 0x01, 0x32, 0x01, 0x61, 0x01, 0x86, 0x01, 0xBF, 0x01, 0xEC, 0x02, 0x31, 0x02, 0x68, 0x02, 0x6A, 0x02, 0x9C, 0x02, 0xCE, 0x02, 0xED, 0x03, 0x13, 0x03, 0x2B, 0x03, 0x49, 0x03, 0x5B, 0x03, 0x6F, 0x03, 0x77, 0x03, 0x81, 0x03, 0x84, 0x03, 0xFF}},

{0xD3, 52,{0x00, 0x63, 0x00, 0x8A, 0x00, 0xB9, 0x00, 0xD6, 0x00, 0xEF, 0x01, 0x15, 0x01, 0x32, 0x01, 0x61, 0x01, 0x86, 0x01, 0xBF, 0x01, 0xEC, 0x02, 0x31, 0x02, 0x68, 0x02, 0x6A, 0x02, 0x9C, 0x02, 0xCE, 0x02, 0xED, 0x03, 0x13, 0x03, 0x2B, 0x03, 0x49, 0x03, 0x5B, 0x03, 0x6F, 0x03, 0x77, 0x03, 0x81, 0x03, 0x84, 0x03, 0xFF}},

{0xD4, 52,{0x00, 0x63, 0x00, 0x8A, 0x00, 0xB9, 0x00, 0xD6, 0x00, 0xEF, 0x01, 0x15, 0x01, 0x32, 0x01, 0x61, 0x01, 0x86, 0x01, 0xBF, 0x01, 0xEC, 0x02, 0x31, 0x02, 0x68, 0x02, 0x6A, 0x02, 0x9C, 0x02, 0xCE, 0x02, 0xED, 0x03, 0x13, 0x03, 0x2B, 0x03, 0x49, 0x03, 0x5B, 0x03, 0x6F, 0x03, 0x77, 0x03, 0x81, 0x03, 0x84, 0x03, 0xFF}},

{0xD5, 52,{0x00, 0x63, 0x00, 0x8A, 0x00, 0xB9, 0x00, 0xD6, 0x00, 0xEF, 0x01, 0x15, 0x01, 0x32, 0x01, 0x61, 0x01, 0x86, 0x01, 0xBF, 0x01, 0xEC, 0x02, 0x31, 0x02, 0x68, 0x02, 0x6A, 0x02, 0x9C, 0x02, 0xCE, 0x02, 0xED, 0x03, 0x13, 0x03, 0x2B, 0x03, 0x49, 0x03, 0x5B, 0x03, 0x6F, 0x03, 0x77, 0x03, 0x81, 0x03, 0x84, 0x03, 0xFF}},

{0xD6, 52,{0x00, 0x63, 0x00, 0x8A, 0x00, 0xB9, 0x00, 0xD6, 0x00, 0xEF, 0x01, 0x15, 0x01, 0x32, 0x01, 0x61, 0x01, 0x86, 0x01, 0xBF, 0x01, 0xEC, 0x02, 0x31, 0x02, 0x68, 0x02, 0x6A, 0x02, 0x9C, 0x02, 0xCE, 0x02, 0xED, 0x03, 0x13, 0x03, 0x2B, 0x03, 0x49, 0x03, 0x5B, 0x03, 0x6F, 0x03, 0x77, 0x03, 0x81, 0x03, 0x84, 0x03, 0xFF}},

{0xF0, 5,{0x55, 0xAA, 0x52, 0x08, 0x03}},

{0xB0, 7,{0x05, 0x15, 0xF5, 0x27, 0x00, 0x00, 0x30}},

{0xB2, 9,{0xF9, 0xFA, 0xFB, 0xFC, 0xF0, 0x00, 0x00, 0xC5, 0x08}},

{0xB3, 6,{0x5B, 0x00, 0xF9, 0x58, 0x26, 0x00}},

{0xB4, 11,{0xFD, 0xFE, 0xFF, 0x00, 0xFC, 0x40, 0x05, 0x08, 0x00, 0x00, 0x00}},

{0xB5, 11,{0x40, 0x00, 0xFD, 0x83, 0x5C, 0x23, 0x24, 0x25, 0x33, 0x33, 0x00}},

{0xB6, 7,{0x81, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00}},

{0xB7, 8,{0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00}},

{0xB8, 3,{0x00, 0x00, 0x00}},

{0xB9, 1,{0x82}},

{0xBA, 16,{0x54, 0xFF, 0xB9, 0xFD, 0x13, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x20, 0xCE, 0x8A, 0xFF, 0x45}},

{0xBB, 16,{0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF}},

{0xBC, 4,{0xF0, 0x3F, 0xFC, 0x0F}},

{0xBD, 4,{0xFF, 0xFF, 0xFF, 0xFF}},

{0xF0, 5,{0x55, 0xAA, 0x52, 0x08, 0x00}},

{0xB0, 2,{0x00, 0x10}},

{0xB4, 1,{0x10}},

{0xB8, 4,{0x01, 0x03, 0x03, 0x03}},

{0xBC, 1,{0x02}},//00

{0xB5, 1,{0x6B}},

{0x35, 1,{0x01}},

{0x11, 0,{0x00}},

{REGFLAG_DELAY, 120, {}},

{0x29, 0,{0x00}},

{REGFLAG_DELAY, 100, {}},

{REGFLAG_END_OF_TABLE, 0x00, {}}
};
#if 0
static struct LCM_setting_table lcm_sleep_out_setting[] = {
    // Sleep Out
    {0x11, 1, {0x00}},
    {REGFLAG_DELAY, 120, {}},
    
    // Display ON
    {0x29, 1, {0x00}},
 {REGFLAG_DELAY, 10, {}},
    {REGFLAG_END_OF_TABLE, 0x00, {}}
};
#endif

static struct LCM_setting_table lcm_deep_sleep_mode_in_setting[] = {
    // Display off sequence
    {0x28, 1, {0x00}},
    {REGFLAG_DELAY, 10, {}},
    
    // Sleep Mode On
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


	params->dsi.mode   = SYNC_EVENT_VDO_MODE;




	// DSI
	/* Command mode setting */
	params->dsi.LANE_NUM				= LCM_TWO_LANE;
	//The following defined the fomat for data coming from LCD engine.
	params->dsi.data_format.color_order = LCM_COLOR_ORDER_RGB;
	params->dsi.data_format.trans_seq   = LCM_DSI_TRANS_SEQ_MSB_FIRST;
	params->dsi.data_format.padding     = LCM_DSI_PADDING_ON_LSB;
	params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

	// Highly depends on LCD driver capability.
	// Not support in MT6573
	params->dsi.packet_size=256;

	// Video mode setting		
	params->dsi.intermediat_buffer_num = 0;

	params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;

	params->dsi.vertical_sync_active				= 2;//4;
	params->dsi.vertical_backporch					= 14;//10;
	params->dsi.vertical_frontporch					= 18;//10;
	params->dsi.vertical_active_line				= FRAME_HEIGHT; 

	params->dsi.horizontal_sync_active				= 8;//4;
	params->dsi.horizontal_backporch				= 16;//32;//50
	params->dsi.horizontal_frontporch				= 24;//32;//50
	params->dsi.horizontal_active_pixel				= FRAME_WIDTH;

		params->dsi.PLL_CLOCK = 230;
		params->dsi.ssc_disable					= 1;
#if 0
	// Bit rate calculation
	//params->dsi.pll_div1=37;		// fref=26MHz, fvco=fref*(div1+1)	(div1=0~63, fvco=500MHZ~1GHz)
	//params->dsi.pll_div2=1; 		// div2=0~15: fout=fvo/(2*div2)

	// Bit rate calculation
	params->dsi.pll_div1=1;		// fref=26MHz, fvco=fref*(div1+1)	(div1=0~63, fvco=500MHZ~1GHz)
	params->dsi.pll_div2=1; 		// div2=0~15: fout=fvo/(2*div2)

	params->dsi.fbk_div =28; // fref=26MHz, fvco=fref*(fbk_div+1)*fbk_sel_real/(div1_real*div2_real) 
#endif
}




static void lcm_init(void)
{
	lcd_power_en(1);
	MDELAY(50);

	SET_RESET_PIN(1);
	MDELAY(10);
	SET_RESET_PIN(0);
	MDELAY(40);
	SET_RESET_PIN(1);
	MDELAY(40);

	push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);
}


static void lcm_suspend(void)
{
	push_table(lcm_deep_sleep_mode_in_setting, sizeof(lcm_deep_sleep_mode_in_setting) / sizeof(struct LCM_setting_table), 1);

}

//static unsigned int lcm_compare_id(void);
//static int vcom = 0x50;
static void lcm_resume(void)
{
	//push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);
	lcm_init();

#if 0
    	#if defined(BUILD_LK)
	//printf("anan_lcd_id =%x\n", lcd_id);	
	printf("anan_lcd_id_pin1 =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	
	//printf("anan_lcd_id_pin2 =%x\n", mt_get_gpio_in(GPIO_LCD_ID2_PIN));
#else
	//printk("anan_lcd_id =%x\n", lcd_id);	
	printk("anan_lcd_id_pin1 =%x\n", mt_get_gpio_in(GPIO_LCD_ID1_PIN));	
	//printk("anan_lcd_id_pin2 =%x\n", mt_get_gpio_in(GPIO_LCD_ID2_PIN));	
#endif
/*
lcm_initialization_setting[14].para_list[1] = vcom;
	SET_RESET_PIN(1);
	MDELAY(10);
	SET_RESET_PIN(0);
	MDELAY(30);
	SET_RESET_PIN(1);
	MDELAY(100);
	push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);
#if defined(BUILD_LK)	
    printf("anan vcom = %x\n", vcom);	
    printf("anan vcom = %x\n", vcom);
#else	
    printk("anan vcom = %x\n", vcom);	
    printk("anan vcom = %x\n", vcom);	
#endif 
	vcom += 2;
*/
#endif
}
         
#if 0
static void lcm_update(unsigned int x, unsigned int y,
                       unsigned int width, unsigned int height)
{
    unsigned int x0 = x;
    unsigned int y0 = y;
    unsigned int x1 = x0 + width - 1;
    unsigned int y1 = y0 + height - 1;
    
    unsigned char x0_MSB = ((x0>>8)&0xFF);
    unsigned char x0_LSB = (x0&0xFF);
    unsigned char x1_MSB = ((x1>>8)&0xFF);
    unsigned char x1_LSB = (x1&0xFF);
    unsigned char y0_MSB = ((y0>>8)&0xFF);
    unsigned char y0_LSB = (y0&0xFF);
    unsigned char y1_MSB = ((y1>>8)&0xFF);
    unsigned char y1_LSB = (y1&0xFF);
    
    unsigned int data_array[16];
    

    data_array[0]= 0x00053902;
    data_array[1]= (x1_MSB<<24)|(x0_LSB<<16)|(x0_MSB<<8)|0x2a;
    data_array[2]= (x1_LSB);
    dsi_set_cmdq(data_array, 3, 1);

    data_array[0]= 0x00053902;
    data_array[1]= (y1_MSB<<24)|(y0_LSB<<16)|(y0_MSB<<8)|0x2b;
    data_array[2]= (y1_LSB);
    dsi_set_cmdq(data_array, 3, 1);
    
    data_array[0]= 0x002c3909;
    dsi_set_cmdq(data_array, 1, 0);
}


static void lcm_setbacklight(unsigned int level)
{
    unsigned int data_array[16];


#if defined(BUILD_LK)
    printf("%s, %d\n", __func__, level);
#elif defined(BUILD_UBOOT)
    printf("%s, %d\n", __func__, level);
#else
    printk("lcm_setbacklight = %d\n", level);
#endif
  
    if(level > 255) 
        level = 255;
    
    data_array[0]= 0x00023902;
    data_array[1] =(0x51|(level<<8));
    dsi_set_cmdq(data_array, 2, 1);
}


static void lcm_setpwm(unsigned int divider)
{
    // TBD
}


static unsigned int lcm_getpwm(unsigned int divider)
{
    // ref freq = 15MHz, B0h setting 0x80, so 80.6% * freq is pwm_clk;
    // pwm_clk / 255 / 2(lcm_setpwm() 6th params) = pwm_duration = 23706
    unsigned int pwm_clk = 23706 / (1<<divider);	


    return pwm_clk;
}
#endif

static unsigned int lcm_compare_id(void)
{
/*
	unsigned int id,id0,id1 = 0;
	unsigned char buffer[3];
	unsigned int  array[16];


	SET_RESET_PIN(1);  //NOTE:should reset LCM firstly
	SET_RESET_PIN(0);
	MDELAY(10);
	SET_RESET_PIN(1);
	MDELAY(120);
	array[0] = 0x00063902;                                                         
	array[1] = 0x52AA55F0;                                                         
	array[2] = 0x00000108;                                                         
	dsi_set_cmdq(&array,3,1);                                                      

	array[0] = 0x00023700;// read id return two byte,version and id
	dsi_set_cmdq(array, 1, 1);
	read_reg_v2(0xc5, buffer, 2);

	array[0] = 0x00023700;// read id return two byte,version and id
	dsi_set_cmdq(array, 1, 1);
	read_reg_v2(0xC5, buffer, 2);
	id0 = buffer[0]; //we only need ID
	id1 = buffer[1]; //we only need ID
        
#if defined(BUILD_LK)
	printf("rm68171 id0=%x,id1=%x \r\n",id0,id1);
#endif
	id =(id0&0x00ff)<<8|(id1&0x00ff);
	  
#ifdef BUILD_LK
	printf("LCM_ID should anan be %x, read_id = 0x%x\n", LCM_ID, id);   
#else
	printk("LCM_ID should anan be %x, read_id = 0x%x\n", LCM_ID, id);  
#endif

  return (id  == LCM_ID)?1:0;
*/
#ifdef BUILD_LK
	unsigned int lcd_id = 0;

	lcd_power_en(1);
	MDELAY(50);

   lcd_id =mt_get_gpio_in(GPIO_LCD_ID1_PIN);

	   return (lcd_id  == 0)? 1:0;
#else
	return 1;
#endif
}


LCM_DRIVER rm68172_fwvga_dsi_vdo_tm_baoxu_tm045ydh2600_mx2103_smt_lcm_drv = 
{
    	.name			= "rm68172_fwvga_dsi_vdo_tm_baoxu_tm045ydh2600_mx2103_smt",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id    = lcm_compare_id,
};

