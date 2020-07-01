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
////////////////////////////////////////     debug1
#ifndef BUILD_LK
#include <linux/kobject.h>
#include <linux/sysfs.h>

#include <linux/string.h>
#include <linux/slab.h>
#endif
////////////////////////////////////////     debug1
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
{0xFF,03,{0x98,0x81,0x03}},

//GIP_1
{0x01,01,{0x00}},    
{0x02,01,{0x00}},
{0x03,01,{0x73}},
{0x04,01,{0x73}},
{0x05,01,{0x00}},
{0x06,01,{0x06}},
{0x07,01,{0x02}},
{0x08,01,{0x00}},
{0x09,01,{0x01}},
{0x0a,01,{0x01}},
{0x0b,01,{0x01}},
{0x0c,01,{0x01}},
{0x0d,01,{0x01}},
{0x0e,01,{0x01}},
{0x0f,01,{0x01}},
{0x10,01,{0x01}},
{0x11,01,{0x00}},
{0x12,01,{0x00}},
{0x13,01,{0x01}},
{0x14,01,{0x00}},
{0x15,01,{0x00}},
{0x16,01,{0x00}}, 
{0x17,01,{0x00}},
{0x18,01,{0x00}},
{0x19,01,{0x00}},
{0x1a,01,{0x00}},
{0x1b,01,{0x00}},
{0x1c,01,{0x00}},
{0x1d,01,{0x00}},
{0x1e,01,{0xC0}},
{0x1f,01,{0x80}},
{0x20,01,{0x04}},
{0x21,01,{0x03}},
{0x22,01,{0x00}},
{0x23,01,{0x00}},
{0x24,01,{0x00}},
{0x25,01,{0x00}},
{0x26,01,{0x00}},
{0x27,01,{0x00}},
{0x28,01,{0x33}},
{0x29,01,{0x03}},
{0x2a,01,{0x00}},
{0x2b,01,{0x00}},
{0x2c,01,{0x00}},
{0x2d,01,{0x00}},
{0x2e,01,{0x00}},
{0x2f,01,{0x00}},
{0x30,01,{0x00}},
{0x31,01,{0x00}},
{0x32,01,{0x00}},
{0x33,01,{0x00}},  
{0x34,01,{0x03}},
{0x35,01,{0x00}},
{0x36,01,{0x03}},
{0x37,01,{0x00}},
{0x38,01,{0x00}},
{0x39,01,{0x00}},
{0x3a,01,{0x00}}, 
{0x3b,01,{0x00}},
{0x3c,01,{0x00}},
{0x3d,01,{0x00}},
{0x3e,01,{0x00}},
{0x3f,01,{0x00}},
{0x40,01,{0x00}},
{0x41,01,{0x00}},
{0x42,01,{0x00}},
{0x43,01,{0x00}}, 
{0x44,01,{0x00}},


//GIP_2{0x  }},
{0x50,01,{0x01}},
{0x51,01,{0x23}},
{0x52,01,{0x45}},
{0x53,01,{0x67}},
{0x54,01,{0x89}},
{0x55,01,{0xab}},
{0x56,01,{0x01}},
{0x57,01,{0x23}},
{0x58,01,{0x45}},
{0x59,01,{0x67}},
{0x5a,01,{0x89}},
{0x5b,01,{0xab}},
{0x5c,01,{0xcd}},
{0x5d,01,{0xef}},

//GIP_3{0x  }},
{0x5e,01,{0x10}},
{0x5f,01,{0x09}},
{0x60,01,{0x08}},
{0x61,01,{0x0F}},
{0x62,01,{0x0E}},
{0x63,01,{0x0D}},
{0x64,01,{0x0C}},
{0x65,01,{0x02}},
{0x66,01,{0x02}},
{0x67,01,{0x02}},
{0x68,01,{0x02}},
{0x69,01,{0x02}},
{0x6a,01,{0x02}},
{0x6b,01,{0x02}},
{0x6c,01,{0x02}},
{0x6d,01,{0x02}},
{0x6e,01,{0x02}},
{0x6f,01,{0x02}},
{0x70,01,{0x02}},
{0x71,01,{0x06}},
{0x72,01,{0x07}},
{0x73,01,{0x02}},
{0x74,01,{0x02}},
{0x75,01,{0x06}},
{0x76,01,{0x07}},
{0x77,01,{0x0E}},
{0x78,01,{0x0F}},
{0x79,01,{0x0C}},
{0x7a,01,{0x0D}},
{0x7b,01,{0x02}},
{0x7c,01,{0x02}},
{0x7d,01,{0x02}},
{0x7e,01,{0x02}},
{0x7f,01,{0x02}},
{0x80,01,{0x02}},
{0x81,01,{0x02}},
{0x82,01,{0x02}},
{0x83,01,{0x02}},
{0x84,01,{0x02}},
{0x85,01,{0x02}},
{0x86,01,{0x02}},
{0x87,01,{0x09}},
{0x88,01,{0x08}},
{0x89,01,{0x02}},
{0x8A,01,{0x02}},

//CMD_P{0xag}},e 4
{0xFF,03,{0x98,0x81,0x04}},

//{00,0{0x1,}},00 //00=3 Lane 80=4 Lane
{0x6C,01,{0x15}},               //Set VCORE voltage =1.5V
{0x6E,01,{0x2A}},               //di_pwr_reg=0 for power mode 2A //VGH clamp 15V
{0x6F,01,{0x35}},               //57   // reg vcl + pumping ratio VGH=3.5x VGL=-2.5x
{0x3A,01,{0xA4}},                
{0x8D,01,{0x1A}},               //VGL clamp -11V
{0x87,01,{0xBA}},                              
{0x26,01,{0x76}},            
{0xB2,01,{0xD1}},

//CMD_P{0xag}},e 1
{0xFF,03,{0x98,0x81,0x01}},
{0x22,01,{0x09}},		//BGR, SS  0x0A->0x09(20160909)
{0x31,01,{0x00}},		//inversion
{0x53,01,{0x20}}, 		//VCOM1  47
{0x55,01,{0x20}},		//VCOM2
{0x50,01,{0xAF}}, 		//VREG1OUT=4.7V  
{0x51,01,{0xAF}}, 		//VREG2OUT=--4.7V  
{0x60,01,{0x14}},               //SDT 

{0xA0,01,{0x08}}, //00		//VP255	Gamma P  0
{0xA1,01,{0x1D}}, //22               //VP251          4
{0xA2,01,{0x2C}}, //36               //VP247          8
{0xA3,01,{0x14}}, //18               //VP243          12
{0xA4,01,{0x19}}, //1E               //VP239          16
{0xA5,01,{0x2E}}, //32               //VP231          24
{0xA6,01,{0x22}}, //27               //VP219          36
{0xA7,01,{0x23}}, //25               //VP203          52
{0xA8,01,{0x97}}, //A6               //VP175          80
{0xA9,01,{0x1E}}, //1D               //VP144          111
{0xAA,01,{0x29}}, //28               //VP111          144
{0xAB,01,{0x7B}}, //7D               //VP80           175
{0xAC,01,{0x18}}, //1A               //VP52           203
{0xAD,01,{0x17}}, //19               //VP36           219
{0xAE,01,{0x4B}}, //4D               //VP24           231
{0xAF,01,{0x1F}}, //23               //VP16           239
{0xB0,01,{0x27}}, //2B               //VP12           243
{0xB1,01,{0x52}}, //49               //VP8            247
{0xB2,01,{0x63}}, //55               //VP4            251
{0xB3,01,{0x39}}, //23               //VP0            255
                                    
{0xC0,01,{0x08}}, //00		     //VN255 GAMMA N
{0xC1,01,{0x1D}}, //22               //VN251        
{0xC2,01,{0x2C}}, //36               //VN247        
{0xC3,01,{0x14}}, //18               //VN243        
{0xC4,01,{0x19}}, //1E               //VN239        
{0xC5,01,{0x2E}}, //32               //VN231        
{0xC6,01,{0x22}}, //27               //VN219        
{0xC7,01,{0x23}}, //25               //VN203        
{0xC8,01,{0x97}}, //A6               //VN175        
{0xC9,01,{0x1E}}, //1D               //VN144        
{0xCA,01,{0x29}}, //28               //VN111        
{0xCB,01,{0x7B}}, //7D               //VN80         
{0xCC,01,{0x18}}, //1A               //VN52         
{0xCD,01,{0x17}}, //19               //VN36         
{0xCE,01,{0x4B}}, //4D               //VN24         
{0xCF,01,{0x1F}}, //23               //VN16         
{0xD0,01,{0x27}}, //2B               //VN12         
{0xD1,01,{0x52}}, //49               //VN8          
{0xD2,01,{0x63}}, //55               //VN4          
{0xD3,01,{0x39}}, //14               //VN0  

//CMD_Page 0


{0xFF,3,{0x98,0x81,0x00}},
//{0x3A,1,{0x77}},
{0x11,1,{0x00}},
{REGFLAG_DELAY,120,{}},
{0x29,1,{0x00}},//Display ON 
{0x35,1,{0x00}},
{REGFLAG_DELAY,20,{}},	


{REGFLAG_END_OF_TABLE, 0x00, {}}

};

/////////////////////////////////////////     debug2
#ifndef BUILD_LK
int ret;
static int j = 0;
static int i = 0;
static int lcm_init_flag = 0;
static int page_mode = 0;	//1 :have page     0 :no page

static struct kobject *properties_kobj;

char *cmd_char = NULL;
unsigned char reg = 0;
unsigned char cmd_count = 0;
unsigned char para_list[64] = {0};

int table_max;

int current_i = 0;
unsigned char current_para_list[64] = {0};


unsigned int id1;
unsigned int id2;



int transform_str_to_int(unsigned char *integer, char *str, unsigned char type)
{
	unsigned char high,low;
	//printf("%c\n",str[0]);
	//printf("%c\n",str[1]);
	switch(str[0])
	{
		case '0': case '1': case'2': case '3': case'4': case '5': case'6': case '7': case'8': case '9':
		high = str[0] - '0';
		break;
		case 'A': case'B': case 'C': case'D': case 'E': case'F': 
		high = str[0] - 'A' + 10;
		break;
		case 'a': case'b': case 'c': case'd': case 'e': case'f': 
		high = str[0] - 'a' + 10;
		break;
	}
	switch(str[1])
	{
		case '0': case '1': case'2': case '3': case'4': case '5': case'6': case '7': case'8': case '9':
		low = str[1] - '0';
		break;
		case 'A': case'B': case 'C': case'D': case 'E': case'F': 
		low = str[1] - 'A' + 10;
		break;
		case 'a': case'b': case 'c': case'd': case 'e': case'f': 
		low = str[1] - 'a' + 10;
		break;
	}

	*integer = (high * type + low);

	return 0;
}



static ssize_t show_reg(struct kobject *kobj, struct kobj_attribute *attr, char *buf)
{
   return sprintf(buf, "cmd : %s\n", cmd_char);
}

static ssize_t store_reg(struct kobject *kobj, struct kobj_attribute *attr, const char *buf, size_t count)
{
   sscanf(buf, "%s", cmd_char);
   ret = transform_str_to_int(&reg,cmd_char,16);
   ret = transform_str_to_int(&cmd_count,(cmd_char+2),10);
   for(j = 0;j < cmd_count;j++)
   {
		ret = transform_str_to_int(&para_list[j],(cmd_char+(j*2+4)),16);
   }
   
   dsi_set_cmdq_V2(reg, cmd_count, para_list, 1);
   
   
   
   ///更新初始化列表的内容
   
	//如果有分不同page
	if(page_mode == 1)
	{
		//如果是更换page
		if(reg == 0xff)
		{
			memcpy(current_para_list,para_list,(64 * sizeof(unsigned char)));
		}
		//否则更新寄存器的参数列表
		else
		{
		   for(i = 0;i < table_max;)
		   {
			   //找到相同page位置
			   if( (lcm_initialization_setting[i].cmd == 0xff) && (0 == memcmp(current_para_list,lcm_initialization_setting[i].para_list,(64 * sizeof(unsigned char))) ) )
			   {
				   i++;
				   for(;lcm_initialization_setting[i].cmd != 0xff;i++)
				   {
					   if(lcm_initialization_setting[i].cmd == reg)
					   {
						   //更新初始化列表的内容
						   lcm_initialization_setting[i].count = cmd_count;
						   memcpy(lcm_initialization_setting[i].para_list,para_list,(64 * sizeof(unsigned char)));
					   }
				   }
			   }
			   else
			   {
				   i++;
			   }
		   }
		}
	}
	//如果没有分page
	else
	{
		for(i = 0;i < table_max;i++)
		{
			if(lcm_initialization_setting[i].cmd == reg)
			{
				//更新初始化列表的内容
				lcm_initialization_setting[i].count = cmd_count;
				memcpy(lcm_initialization_setting[i].para_list,para_list,(64 * sizeof(unsigned char)));
			}
		}
	}
	
	
   
   
   //clear
   memset(para_list,0,sizeof(para_list));
   
   return count;
}


static ssize_t show_id(struct kobject *kobj, struct kobj_attribute *attr, char *buf)
{
   //id1 = mt_get_gpio_in(GPIO_LCD_ID1_PIN); 
   //id2 = mt_get_gpio_in(GPIO_LCD_ID2_PIN);  
   //return sprintf(buf, "%d,%d,%d,%d,%d,%d\n", current_para_list[0],current_para_list[1],current_para_list[2],current_para_list[3],current_para_list[4],current_para_list[5]);
	return 0;
}


static ssize_t show_page(struct kobject *kobj, struct kobj_attribute *attr, char *buf)
{ 
   return sprintf(buf, "%d,%d,%d,%d,%d,%d\n", current_para_list[0],current_para_list[1],current_para_list[2],current_para_list[3],current_para_list[4],current_para_list[5]);
}

static ssize_t show_page_mode(struct kobject *kobj, struct kobj_attribute *attr, char *buf)
{ 
   return sprintf(buf, "page_mode : %d\n",page_mode);
}


static struct kobj_attribute lcm_page_mode_attr = {
	.attr = {
		 .name = "page_mode",
		 .mode = S_IRUGO | S_IWUSR,
		 },
	.show  = &show_page_mode,
};


static struct kobj_attribute lcm_reg_attr = {
	.attr = {
		 .name = "reg",
		 .mode = S_IRUGO | S_IWUSR,
		 },
	.show  = &show_reg,
	.store = &store_reg,
};

static struct kobj_attribute lcm_id_attr = {
	.attr = {
		 .name = "id",
		 .mode = S_IRUGO,
		 },
	.show  = &show_id,
};

static struct kobj_attribute lcm_page_attr = {
	.attr = {
		 .name = "page",
		 .mode = S_IRUGO,
		 },
	.show  = &show_page,
};


static struct attribute *lcm_debug_attrs[] = {
	&lcm_reg_attr.attr,
	&lcm_id_attr.attr,
	&lcm_page_attr.attr,
	&lcm_page_mode_attr.attr,
	NULL
};

static struct attribute_group lcm_debug_attr_group = {
	.attrs = lcm_debug_attrs,
};
#endif
/////////////////////////////////////////     debug2


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
		params->dbi.te_mode 				= LCM_DBI_TE_MODE_DISABLED;  //LCM_DBI_TE_MODE_VSYNC_ONLY;
		//params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

		params->dsi.mode   = SYNC_PULSE_VDO_MODE;

	
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

		
		params->dsi.vertical_sync_active				= 10;
		params->dsi.vertical_backporch				= 20;
		params->dsi.vertical_frontporch					= 10; //20;	//16;
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		params->dsi.horizontal_sync_active				= 40;
		params->dsi.horizontal_backporch				= 100;
		params->dsi.horizontal_frontporch				= 80;
		params->dsi.horizontal_active_pixel 			= FRAME_WIDTH;

/*
		// Bit rate calculation
		params->dsi.pll_div1=1;		// div1=0,1,2,3;div1_real=1,2,4,4
		params->dsi.pll_div2=0;		// div2=0,1,2,3;div2_real=1,2,4,4
		params->dsi.fbk_div =18;		// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)		
*/
	params->dsi.PLL_CLOCK = 195;//221 210; //this value must be in MTK suggested table

		params->dsi.ssc_disable					= 1;
		
		params->dsi.HS_TRAIL =10;//modify for wpf d6001 factory flicker problem [2016-03-24]
		
		//params->dsi.clk_lp_per_line_enable = 0;
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
		
		params->dsi.lcm_esd_check_table[0].cmd 		= 0xac;//0x0A;
		params->dsi.lcm_esd_check_table[0].count 	= 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] = 0x00;//0x9C;
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


////////////////////////////////////////      debug3
#ifndef BUILD_LK
	if(lcm_init_flag == 0)
	{
		properties_kobj = kobject_create_and_add("lcm_debug", NULL);
		ret = sysfs_create_group(properties_kobj, &lcm_debug_attr_group);
		lcm_init_flag = 1;
		cmd_char = kmalloc(133, GFP_KERNEL);
		memset(cmd_char,'\0',sizeof(cmd_char));
	}	
	//遍历整个数组，得到当前是哪个page
	table_max = sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table);
	for(i = 0;i < table_max;i++)
	{
		if( lcm_initialization_setting[i].cmd == 0xff && (lcm_initialization_setting[i].count > 2) )
		{
			if(
			(lcm_initialization_setting[i].para_list[4] != 0) ||
			(lcm_initialization_setting[i].para_list[3] != 0) || 
			(lcm_initialization_setting[i].para_list[2] != 0) || 
			(lcm_initialization_setting[i].para_list[1] != 0) || 
			(lcm_initialization_setting[i].para_list[0] != 0)
			)
			{
				memcpy(current_para_list,lcm_initialization_setting[i].para_list,(64 * sizeof(unsigned char)));
				page_mode = 1;
			}
		}
	}
#endif
////////////////////////////////////////      debug3


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


LCM_DRIVER ili9881c_hd720_dsi_vdo_ivo_bx_bx55003139c_mx2120e_ber_s5566_lcm_drv = 
{
    .name			= "ili9881c_hd720_dsi_vdo_ivo_bx_bx55003139c_mx2120e_ber_s5566",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
};
