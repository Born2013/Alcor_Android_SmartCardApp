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
#include <platform/mt_gpio.h>
#else
#include <linux/gpio.h>
#endif

// ---------------------------------------------------------------------------
//  Local Constants
// ---------------------------------------------------------------------------

#define FRAME_WIDTH  (480)
#define FRAME_HEIGHT (854)

#define REGFLAG_DELAY             							0XFC
#define REGFLAG_END_OF_TABLE      							0xFD   // END OF REGISTERS MARKER

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
       

 struct LCM_setting_table {
    unsigned cmd;
    unsigned char count;
    unsigned char para_list[64];
};

/*
static struct LCM_setting_table lcm_set_window[] = {
	{0x2A,	4,	{0x00, 0x00, (FRAME_WIDTH>>8), (FRAME_WIDTH&0xFF)}},
	{0x2B,	4,	{0x00, 0x00, (FRAME_HEIGHT>>8), (FRAME_HEIGHT&0xFF)}},
	{REGFLAG_END_OF_TABLE, 0x00, {}}
};


static struct LCM_setting_table lcm_sleep_out_setting[] = {
    	// Sleep Out
	{0x11, 1, {0x00}},
    	{REGFLAG_DELAY, 120, {}},
    
    	// Display ON
	{0x29, 1, {0x00}},
	{REGFLAG_DELAY, 10, {}},
	
	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
*/

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

        {0x10,1,{0x00}},   
        {REGFLAG_DELAY,200,{}},

	{0xFF,5,{0x77,0x01,0x00,0x00,0x11}},
	{0xD1,1,{0x11}},

	{0x11,1,{0x00}},                 // Sleep-Out

	{REGFLAG_DELAY,200,{}},
		
	{0xFF,5,{0x77,0x01,0x00,0x00,0x10}},
	{0xC0,2,{0xe9,0x03}},
	{0xC1,2,{0x11,0x02}},
	{0xC2,2,{0x30,0x08}},
	{0xB0,16,{0x00,0x06,0x11,0x12,0x18,0x0A,0x0A,0x09,0x09,0x1D,0x09,0x14,0x10,0x0E,0x11,0x19}},
	{0xB1,16,{0x00,0x06,0x11,0x11,0x15,0x09,0x0B,0x09,0x09,0x23,0x09,0x17,0x14,0x18,0x1E,0x19}},
	//-------------------------------- Power Control Registers Initial --------------------------------------//
	{0xFF,5,{0x77,0x01,0x00,0x00,0x11}},
	{0xb0,1,{0x4d}},//4d//5f
	//-------------------------------------------Vcom Setting---------------------------------------------------//
	{0xb1,1,{0x35}},//5a//3A
	//-----------------------------------------End Vcom Setting-----------------------------------------------//
	{0xb2,1,{0x07}},//07
	{0xb3,1,{0x80}},
	{0xb5,1,{0x47}},//43
	{0xb7,1,{0x8a}},
	{0xb8,1,{0x21}},
	{0xC1,1,{0x78}},
	{0xC2,1,{0x78}},
	{0xD0,1,{0x88}},
	//---------------------------------End Power Control Registers Initial -------------------------------//
	{REGFLAG_DELAY,100,{}},
	//---------------------------------------------GIP Setting----------------------------------------------------//
	{0xe0,3,{0x00,0x00,0x02}},
	{0xe1,11,{0x08,0x00,0x0A,0x00,0x07,0x00,0x09,0x00,0x00,0x33,0x33}},
	{0xe2,13,{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},
	{0xe3,4,{0x00,0x00,0x33,0x33}},
	{0xe4,2,{0x44,0x44}},
	{0xe5,16,{0x0E,0x60,0xAF,0xAF,0x10,0x60,0xAF,0xAF,0x0A,0x60,0xAF,0xAF,0x0C,0x60,0xAF,0xAF}},
	{0xe6,4,{0x00,0x00,0x33,0x33}},
	{0xe7,2,{0x44,0x44}},
	{0xe8,16,{0x0D,0x60,0xAF,0xAF,0x0F,0x60,0xAF,0xAF,0x09,0x60,0xAF,0xAF,0x0B,0x60,0xAF,0xAF}},
	{0xeb,7,{0x02,0x01,0xE4,0xE4,0x44,0x00,0x40}},
	{0xeC,2,{0x02,0x01}},
	{0xed,16,{0xAB,0x89,0x76,0x54,0x01,0xff,0xff,0xff,0xff,0xff,0xff,0x10,0x45,0x67,0x98,0xBA}},
	//---------------------------------------------End GIPSetting-----------------------------------------------//
	//------------------------------ Power Control Registers Initial End-----------------------------------//
	//------------------------------------------Bank1 Setting----------------------------------------------------//
	{0xFF,5,{0x77,0x01,0x00,0x00,0x00}},

	{REGFLAG_DELAY,10,{}},	

	{0x29,1,{0x00}},                 // Display On
	{REGFLAG_DELAY,100,{}},	

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
	params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

	params->dsi.mode   = SYNC_PULSE_VDO_MODE;


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

	params->dsi.intermediat_buffer_num = 0;

	params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;
	params->dsi.word_count=480*3;	

	params->dsi.vertical_sync_active			= 2;//1010
	params->dsi.vertical_backporch				= 19;//10 30
	params->dsi.vertical_frontporch				= 8;//30 30 
	params->dsi.vertical_active_line			= FRAME_HEIGHT;

	params->dsi.horizontal_sync_active			= 8;//4 60
	params->dsi.horizontal_backporch			= 40;//32 80
	params->dsi.horizontal_frontporch			= 30;//32 80
	params->dsi.horizontal_active_pixel			= FRAME_WIDTH;


	params->dsi.PLL_CLOCK = 175;//157; this value must be in MTK suggested table
	params->dsi.ssc_disable					= 0;
	//params->dsi.clk_lp_per_line_enable = 0;

	params->dsi.esd_check_enable = 1;
	params->dsi.customization_esd_check_enable = 1;
	
	params->dsi.lcm_esd_check_table[0].cmd          = 0x0a;//0a
	params->dsi.lcm_esd_check_table[0].count        = 1;
	params->dsi.lcm_esd_check_table[0].para_list[0] = 0x9c;//9c
	
	params->dsi.noncont_clock=1;
	params->dsi.noncont_clock_period=2;
}


static void lcm_init(void)
{

	SET_RESET_PIN(1);
	MDELAY(2);
	SET_RESET_PIN(0);
	MDELAY(10);
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
  	MDELAY(150);

}


static void lcm_resume(void)
{

//  lcm_initialization_setting[14].para_list[0]+=1;
//	lcm_initialization_setting[15].para_list[0]+=1;
	lcm_init();
//	push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);
}

static unsigned int lcm_compare_id(void)
{
#ifdef BUILD_LK
	unsigned int lcd_id1,lcd_id2 = 0;
	lcd_id1 = mt_get_gpio_in(GPIO_LCD_ID1_PIN);
	lcd_id2 = mt_get_gpio_in(GPIO_LCD_ID2_PIN);
	if((lcd_id1==1)&&(lcd_id2==1))
		return 1;
	else
		return 0;
#else
		return 1;
#endif
}

LCM_DRIVER st7701_fwvga_hsdtn_vdo_yixindag5_for_e5011_lcm_drv = 
{
    	.name			= "st7701_fwvga_hsdtn_vdo_yixindag5_for_e5011",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
};
