/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
 
/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2008
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/

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

#define REGFLAG_DELAY             							0XF1
#define REGFLAG_END_OF_TABLE      							0x100   // END OF REGISTERS MARKER

//#define LCM_ID_JD9365   0x9365
#define LCM_DSI_CMD_MODE                                    0

// ---------------------------------------------------------------------------
//  Local Variables
// ---------------------------------------------------------------------------
//static unsigned int lcm_esd_test = FALSE; ///only for ESD test
//static unsigned int lcm_check_status(void);
static LCM_UTIL_FUNCS lcm_util = {0};

#define SET_RESET_PIN(v)    (lcm_util.set_reset_pin((v)))


#define UDELAY(n) (lcm_util.udelay(n))
#define MDELAY(n) (lcm_util.mdelay(n))



#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)    lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update)
#define dsi_set_cmdq(pdata, queue_size, force_update)        lcm_util.dsi_set_cmdq(pdata, queue_size, force_update)
#define wrtie_cmd(cmd)                                    lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)                lcm_util.dsi_write_regs(addr, pdata, byte_nums)
#define read_reg                                            lcm_util.dsi_read_reg()
#define read_reg_v2(cmd, buffer, buffer_size)                   lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)  

static void lcd_power_en(unsigned char enabled)
{
	if (enabled)
	{

		pmic_set_register_value(PMIC_RG_VGP1_EN,1);
		pmic_set_register_value(PMIC_RG_VGP1_VOSEL,0x05);
		//pmic_set_register_value(PMIC_RG_VCAMA_EN,1);
		//pmic_set_register_value(PMIC_RG_VCAMA_VOSEL,0x03); //for ctp 1151
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


static void lcm_set_util_funcs(const LCM_UTIL_FUNCS *util)
{
    memcpy(&lcm_util, util, sizeof(LCM_UTIL_FUNCS));
}

static struct LCM_setting_table lcm_initialization_setting[] = {

{0xB9,3,{0xF1,0x12,0x83}},	

{0xBA,27,{0x33,0x81,0x05,0xF9,0x0E,0x0E,0x20,0x00,0x00,0x00,
					0x00,0x00,0x00,0x00,0x44,0x25,0x00,0x91,0x0A,0x00,
					0x00,0x02,0x4F,0xD1,0x00,0x00,0x37}},//BAH 7th = 0x20 (improve coverage with weak FPC)

{0xB8,4,{0x25,0x22,0x20,0x03}},

{0xBF,3,{0x02,0x11,0x00}},

{0xB3,10,{0x0C,0x10,0x0A,0x50,0x03,0xFF,0x00,0x00,0x00,0x00}},

{0xC0,9,{0x73,0x73,0x50,0x50,0x00,0x00,0x08,0x70,0x00}},

{0xBC,1,{0x46}},

{0xCC,1,{0x07}},

{0xB4,1,{0x80}},

{0xB2,3,{0xC8,0x02,0x30}},

{0xE3,14,{0x07,0x07,0x0B,0x0B,0x03,0x0B,0x00,0x00,0x00,0x00,0xFF,0x80,0xC0,0x10}},

{0xC1,12,{0x53,0x00,0x1E,0x1E,0x77,0xC1,0xFF,0xFF,0xCC,0xCC,0x77,0x77}},

{0xB5,2,{0x68,0x68}},

{0xB6,2,{0x70,0x70}},//7c

{0xE9,63,{0xC2,0x10,0x05,0x04,0xFE,0x02,0xA1,0x12,0x31,0x45,
					0x3F,0x83,0x12,0xB1,0x3B,0x2A,0x08,0x05,0x00,0x00,
					0x00,0x00,0x08,0x05,0x00,0x00,0x00,0x00,0xFF,0x02,
					0x46,0x02,0x48,0x68,0x88,0x88,0x88,0x80,0x88,0xFF,
					0x13,0x57,0x13,0x58,0x78,0x88,0x88,0x88,0x81,0x88,
					0x00,0x00,0x00,0x00,0x00,0x12,0xB1,0x3B,0x00,0x00,0x00,0x00,0x00}},

{0xEA,61,{0x00,0x1A,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
				  0x00,0x00,0xFF,0x31,0x75,0x31,0x18,0x78,0x88,0x88,
					0x88,0x85,0x88,0xFF,0x20,0x64,0x20,0x08,0x68,0x88,
					0x88,0x88,0x84,0x88,0x20,0x10,0x00,0x00,0x54,0x00,
					0x00,0x00,0x00,0x00,0x00,0x00,0xC0,0x00,0x00,0x0C,
				  0x00,0x00,0x00,0x00,0x30,0x02,0xA1,0x00,0x00,0x00,0x00}},

{0xE0,34,{0x00,0x05,0x08,0x24,0x35,0x3F,0x37,0x2F,0x07,0x0C,0x0C,0x11,0x13,0x11,0x13,0x10,0x17,
					0x00,0x05,0x08,0x24,0x35,0x3F,0x37,0x2F,0x07,0x0C,0x0C,0x11,0x13,0x11,0x13,0x10,0x17}},

{0X11,0,{0x00}},

{REGFLAG_DELAY, 250, {}},

{0X29,0,{0x00}},
{REGFLAG_DELAY, 50, {}},

	{REGFLAG_END_OF_TABLE,0x00,{}}
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


static void lcm_get_params(LCM_PARAMS *params)
{

  	memset(params, 0, sizeof(LCM_PARAMS));	
		params->type   = LCM_TYPE_DSI;
		params->width  = FRAME_WIDTH;
		params->height = FRAME_HEIGHT;
        #if (LCM_DSI_CMD_MODE)
		params->dsi.mode   = CMD_MODE;
        #else
		params->dsi.mode   = BURST_VDO_MODE;//SYNC_PULSE_VDO_MODE BURST_VDO_MODE  SYNC_PULSE_VDO_MODE;//BURST_VDO_MODE; 
        #endif
	
		// DSI
		/* Command mode setting */
		//1 Three lane or Four lane
		params->dsi.LANE_NUM				= LCM_FOUR_LANE;
		//The following defined the fomat for data coming from LCD engine.
		params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

		// Video mode setting		
		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;
		
		params->dsi.vertical_sync_active				= 3;
    		params->dsi.vertical_backporch					= 11;
		params->dsi.vertical_frontporch 				= 15;
		params->dsi.vertical_active_line				= FRAME_HEIGHT; 

		params->dsi.horizontal_sync_active				= 5;//24
		params->dsi.horizontal_backporch				= 60;//48
		params->dsi.horizontal_frontporch				= 40;//40
		params->dsi.horizontal_active_pixel 			= FRAME_WIDTH;

	  //params->dsi.LPX=8; 
		// Bit rate calculation
		//1 Every lane speed
		//params->dsi.pll_select=1;
		//params->dsi.PLL_CLOCK  = LCM_DSI_6589_PLL_CLOCK_377;
		params->dsi.PLL_CLOCK=225;//175;//208
	//add by tjf start 20160326
#if 0
		params->dsi.esd_check_enable = 1;
		params->dsi.customization_esd_check_enable = 1;
		
		params->dsi.lcm_esd_check_table[0].cmd          = 0x0a;//0a
		params->dsi.lcm_esd_check_table[0].count        = 1;
		params->dsi.lcm_esd_check_table[0].para_list[0] = 0x9c;//9c
#endif		
		//params->dsi.lcm_esd_check_table[0].cmd          = 0xac;//0a
		//params->dsi.lcm_esd_check_table[0].count        = 1;
		//params->dsi.lcm_esd_check_table[0].para_list[0] = 0x00;//9c
		
		//params->dsi.noncont_clock=1;
		//params->dsi.noncont_clock_period=2;
		//add by tjf end
		//params->dsi.compatibility_for_nvk = 1;// this parameter would be set to 1 if DriverIC is NTK's and when force match DSI clock for NTK's
}




static void lcm_init(void)
{
    
    lcd_power_en(1);
    MDELAY(50);
    SET_RESET_PIN(1);
    MDELAY(10);
    SET_RESET_PIN(0);
    MDELAY(20);
    SET_RESET_PIN(1);
    MDELAY(50);
    push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);
}


#if (LCM_DSI_CMD_MODE)
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
    
    data_array[0]= 0x00290508;
    dsi_set_cmdq(data_array, 1, 1);
    
    data_array[0]= 0x002c3909;
    dsi_set_cmdq(data_array, 1, 0);


}
#endif
static void lcm_suspend(void)
{
    unsigned int data_array[16];
    MDELAY(20);
    data_array[0]=0x00E01500;
    dsi_set_cmdq(data_array,1,1);
    
    data_array[0]=0x93E11500;
    dsi_set_cmdq(data_array,1,1);
    
    data_array[0]=0x65E21500;
    dsi_set_cmdq(data_array,1,1); 
    
    data_array[0]=0xF8E31500;
    dsi_set_cmdq(data_array,1,1);
    
    data_array[0]=0x00280500;
    dsi_set_cmdq(data_array,1,1);
    MDELAY(20);
    data_array[0]=0x00100500;
    dsi_set_cmdq(data_array,1,1);
    MDELAY(150);
    

    data_array[0]=0x00E01500;
    dsi_set_cmdq(data_array,1,1);
    
    data_array[0]=0x09E11500;
    dsi_set_cmdq(data_array,1,1);
    
    data_array[0]=0xB1E21500;
    dsi_set_cmdq(data_array,1,1); 
    
    data_array[0]=0x7FE31500;
    dsi_set_cmdq(data_array,1,1);

    SET_RESET_PIN(0);
    MDELAY(200);

}

static void lcm_resume(void)
{
     SET_RESET_PIN(1);
    MDELAY(10);
    SET_RESET_PIN(0);
    MDELAY(20);
    SET_RESET_PIN(1);
    MDELAY(50);
    push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);

  
  //lcm_init();
   /*
   
    unsigned int data_array[16];
    data_array[0]=0x00110500;
    dsi_set_cmdq(data_array,1,1);
    MDELAY(120);
    data_array[0]=0x00290500;
    dsi_set_cmdq(data_array,1,1);
    MDELAY(10);
    */
}

static unsigned int lcm_compare_id(void)
{

	unsigned int lcd_id1,lcd_id2 = 0;
	lcd_power_en(1);
	MDELAY(50);
#ifdef BUILD_LK
	lcd_id1 = mt_get_gpio_in(GPIO_LCD_ID1_PIN);
	lcd_id2 = mt_get_gpio_in(GPIO_LCD_ID2_PIN);
	printf("lcd_id1 == %d , lcd_id2 == %d.\n",lcd_id1, lcd_id2);
#else
	lcd_id1 = gpio_get_value(GPIO19_LCD_ID1_PIN);
	lcd_id2 = gpio_get_value(GPIO20_LCD_ID2_PIN);
	printk(KERN_ERR "lcd_id1 == %d , lcd_id2 == %d.\n",lcd_id1, lcd_id2);
#endif

	
	if((lcd_id1==0)&&(lcd_id2==1))
		return 1;
	else
		return 0;
}


LCM_DRIVER st7703_hd720_dsi_vdo_lg_qt470hd074sh01_mx2521e_yaao_x28_lcm_drv = 
{
        .name   	= "st7703_hd720_dsi_vdo_lg_qt470hd074sh01_mx2521e_yaao_x28",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
#if (LCM_DSI_CMD_MODE)
    .update         = lcm_update,
#endif
};
