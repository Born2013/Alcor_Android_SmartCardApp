#ifndef __GSL_TS_DRIVER_H__
#define __GSL_TS_DRIVER_H__
/*********************************/
#define TPD_HAVE_BUTTON		//按键宏
#define GSL_ALG_ID			//有没有id算法
#define GSL_COMPATIBLE_CHIP	//芯片兼容宏
#define GSL_THREAD_EINT		//线程中断
//#define GSL_DEBUG			//调试
#define TPD_PROC_DEBUG		//adb调试
#if ( defined(MX1091_ARES_A5088_FW) || defined(MX1091_HYF_A1_PRO) || defined(MX1091_ARES_A5502) || defined(VH2510_WPF_D5013_FW) || defined(VH2521_A5590) || defined(VH2521_BANGHUA_D1C) || defined(VH2521E_JC_R21) || defined(VH2521E_ALS_A5039_FW) || defined(VH2521E_ALS_A5039_FW_FOR_HSD) || defined(MX1092_JC_C03_FWVGA) || defined(MX2103_JC_C09_FWVGA) || defined(MX2116_FD_VE4518) || defined(MX2116_BER_S5513)|| defined(MX2116G_FD_VE5056B_HD) ||defined(MX2116_JC_C12) || defined(MX2109F_WPF_D5018) || defined(MX2109F_HDS_X1508)|| defined(MX2116_WPF_D5019) || defined (MX2111_WPF_D5020) || defined (MX2116_XHT_771) || defined(MX2116_NF_S5063_CTA) ||defined(MX2116_WPF_D5015)||defined(MX2116G_BER_S5088_FW)||defined(MX2116G_WPF_D5023_HD720)|| defined (MX2116_JC_C12)||defined (MX2116G_JY_A5009)||defined (MX2116_MJ_6001)||defined(MX2118_BER_S5071)|| defined(MX2118_NF_S5063)||defined (MX2126E_BER_S5518_QHD) ||defined (MX2126E_WPF_D5027_HD720)||defined (MX2126E_WPF_D5027_FWVGA) || defined (MX2118_HYF_JD1) || defined (MX2117L_WPF_D5015E_FWVGA))
#define GSL_TIMER			//定时器宏
#else
//#define GSL_TIMER			//定时器宏
#endif
//#define GSL_IRQ_CHECK

#if defined (BIRD_TPD_PROXIMITY)
#define TPD_PROXIMITY		//距离传感器宏
#else
//#define TPD_PROXIMITY		//距离传感器宏
#endif


//#if 0//defined(MX2116_JC_C12)||defined(MX2116_JC_C11)  add gesture
#if ( defined(MX2120E_BER_5065_HD) || defined(MX2128E_JD_JD5002_HD720)||defined(MX2150E_JY_A5010_FWVGA) )
//#define GSL_GESTURE
#else
#define GSL_GESTURE
#endif
//#define GSL_GPIO_IDT_TP	//GPIO兼容	
#if defined(MX2117L_WPF_D5015E_FWVGA)||defined(MX2116_WPF_D5015)|| defined(MX2116G_BER_S5088_FW)||defined(MX2116G_WPF_D5023_HD720)||defined (MX2116_JC_C12)|| defined(MX2116G_BER_S5089_HD720)|| defined(MX2116G_FD_VE5056)||defined(MX2117_ARES_A5029)||defined(MX2118_NF_S5063)||defined(MX2126E_ARES_A5032) || defined (MX2120E_WPF_D5513_HD) || defined (MX2128E_ALS_A5066_FWVGA) ||defined(MX1091E_WPF_D5512_QHD)|| defined(MX2116G_FD_VE5056B_HD)||defined(MX1091E_WPF_D5512_HD720)||defined(MX1091E_ARES_A5509_HD)||defined(MX1091E_BER_S5067_FW) || defined (MX2118_HYF_JD1)||defined(MX1091E_ARES_A5509_QHD) ||defined(MX1091E_WPF_D5028_FW) ||defined(MX1091E_WPF_D5028B_HD)||defined(MX2118_BER_S5071)||defined(MX2128E_WPF_D5031_FWVGA) ||defined(MX2128E_WPF_D5030B_FWVGA) ||defined(MX2128E_WPF_D5030_HD720)||defined(VH2521E_BANGHUA_U12) || defined(VH2521E_ALS_A5039_FW)||defined(MX2116_JC_C15)||defined(MX2118B_BER_S5071K)||defined(MX2150E_WPF_D5033_FWVGA) ||defined(MX2521E_WPF_D5517) 
#define GSL_DRV_WIRE_IDT_TP	//驱动线兼容
#elif defined (MX2126E_BER_S5518_QHD)||defined (MX2126E_WPF_D5027_HD720)
#define GSL_IDENTY_TP_BY_DAC
#endif

#define GSL9XX_VDDIO_1800 1
#if defined GSL_IDENTY_TP_BY_DAC
    #if defined(MX2126E_BER_S5518_QHD) 
        #define BIRD_LOAD_FW_THRD  0x49
    #elif defined (MX2126E_WPF_D5027_HD720)
		#define BIRD_LOAD_FW_THRD  0x3d
    #else
        #define BIRD_LOAD_FW_THRD  0x36
    #endif  
#endif   

#if ( defined(MX2116_SMT_FW) ||defined(MX2103_SMT_FW)|| defined(MX2116_ARES_A5086_FW)|| defined(MX2116_ARES_A5086_QHD) || defined(MX2116_WPF_D5015)|| defined(MX2116_WPF_D5015_TEST) || defined(MX2116_BER_S4513) || defined(MX2116_BER_S5075) ||defined(MX2116_WPF_D5012)||defined(MX2116_NF_S5051)|| defined(MX2103_JC_C09_FWVGA)|| defined(MX2116_WPF_D5019)||defined (MX2116_ARES_A5083B_QHD) ||defined(MX2109F_ARES_A5085)||defined(MX2116G_BER_S5088_FW)||defined(MX2116G_WPF_D5023_HD720)||defined (MX2116_BER_S5513)|| defined(MX2116G_FD_VE5056B_HD) ||defined(MX2118_BER_S5071)||defined (MX2126E_BER_S5518_QHD) ||defined (MX2126E_WPF_D5027_HD720)||defined (MX2126E_WPF_D5027_FWVGA)||defined(MX2117L_WPF_D5015E_FWVGA))
#define TPD_POWER_VTP28_USE_VCAMA   //[add for mx2116 2015-11-03]
#endif

#define GSL_PAGE_REG    0xf0
#define GSL_CLOCK_REG   0xe4
#define GSL_START_REG   0xe0
#define POWE_FAIL_REG   0xbc
#define TOUCH_INFO_REG  0x80
#define TPD_DEBUG_TIME	0x20130424
struct gsl_touch_info
{
	int x[10];
	int y[10];
	int id[10];
	int finger_num;	
};

struct gsl_ts_data {
	struct i2c_client *client;
	struct workqueue_struct *wq;
	struct work_struct work;
	unsigned int irq;
	//struct early_suspend pm;
};

/*button*/
#if defined (MX1092_ARES_5091)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{80,1030,20,20},{240,1030,20,20},{460,1030,20,20}}
#elif defined(MX1092_HYF_V7)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{80,1030,20,20},{240,1030,20,20},{460,1030,20,20}}
#elif defined(VH2521_A5590)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{100,1000,20,20},{250,1000,30,30},{400,1000,30,30}}
#elif defined(VH2521_BANGHUA_D1C)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,50,30},{324,1320,50,30},{612,1320,50,30}}
#elif defined(MX1092_HYF_A1_FWVGA)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{80,1030,20,20},{240,1030,30,30},{460,1030,20,20}}
#elif defined(MX1092_JY_A5006)//mingwangda gsl968 qhd
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{80,1030,20,20},{240,1030,20,20},{460,1030,20,20}}
#elif defined(MX1091_HYF_A1_PRO)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1317,20,20},{396,1317,30,30},{612,1317,20,20}}
#elif defined(MX1091_ARES_A5502)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{100,1500,20,20},{200,1500,20,20},{300,1500,20,20}}
#elif defined (MX1091_ARES_A5088)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{80,994,20,20},{243,994,30,30},{460,994,20,20}}
#elif defined (MX1091_ARES_A5088_FW)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{460,884,20,20}}
#elif defined (MX2116_SOP_TN_TP_PROXIMITY)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{460,884,20,20}}
#elif defined (MX1091_ARES_A5019)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{80,994,20,20},{243,994,30,30},{460,994,20,20}}
#elif defined (MX1091_ARES_A5019_FW)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{460,884,20,20}}
#elif defined (VH2510_WPF_D5013_FW)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{100,1000,20,20},{250,1000,30,30},{400,1000,20,20}}
#elif defined (VH2510_WPF_D5016)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,990,20,20},{243,990,30,30},{459,990,20,20}}
#elif defined (VH2510_NUOFEI_S5042B)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,1000,20,20},{216,1000,30,30},{408,1000,20,20}}
#elif defined (MX2116_ARES_A5086_FW)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,20,20},{408,884,20,20}}
#elif defined (MX2116_ARES_A5086_QHD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,988,20,20},{243,988,20,20},{459,988,20,20}}
#elif defined(MX2116_WPF_D5015)||defined(MX2117L_WPF_D5015E_FWVGA)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{408,884,20,20}}
#elif defined(MX2116_WPF_D5015_TEST)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{408,884,20,20}}
#elif defined(MX2116_WPF_D5019)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{408,884,20,20}}
#elif defined(MX2116_BER_S4513)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{80,900,60,60},{240,900,60,60},{400,900,60,60}}
#elif defined(MX2116_BER_S4513_QHD)//QHD（81,994）（297,994）（459,994）
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,994,60,60},{297,994,60,60},{459,994,60,60}}
#elif defined(MX2116_BER_S5075)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{80,900,60,60},{240,900,60,60},{400,900,60,60}}
#elif defined (MX2116_WPF_D5012)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{80,1030,20,20},{240,1030,20,20},{460,1030,20,20}}
#elif defined(MX2116_WPF_D4513)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{264,884,20,20},{408,884,20,20}}
#elif defined(MX2116_WPF_D4513_QHD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,994,20,20},{297,994,20,20},{459,994,20,20}}
#elif defined(MX2116_FD_VE4518)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{264,884,20,20},{408,884,20,20}}
#elif defined(MX2116_BER_S5513)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{100,1000,20,20},{250,1000,20,20},{400,1000,20,20}}
#elif defined(MX2109F_ARES_A5085)||defined(MX2109F_ARES_A5085_FOR_MX2132ER)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,20,20},{408,884,20,20}}
#elif defined(MX2109F_ARES_A5085_QHD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,988,20,20},{243,988,20,20},{459,988,20,20}}
#elif defined (MX2116_NF_S5051)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
//#define TPD_KEYS_DIM            {{80,1030,20,20},{240,1030,20,20},{460,1030,20,20}}
#define TPD_KEYS_DIM            {{120,880,20,20},{264,880,20,20},{408,880,20,20}}
#elif defined (MX2116_NF_S5063_CTA)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{120,880,20,20},{264,880,20,20},{408,880,20,20}}
#elif defined (MX1092_JC_C03_FWVGA)  //（72,880）（216,880）（408,880）
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,880,20,20},{216,880,20,20},{408,880,20,20}}
#elif defined (MX2109F_ARES_5503)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,988,20,20},{297,988,20,20},{459,988,20,20}}
#elif defined (MX2103_JC_C09_FWVGA)  //（72,880）（216,880）（408,880）
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,880,20,20},{216,880,20,20},{408,880,20,20}}
#elif defined (MX2116_NF_S5559)	//（81,988）（297,988）（459,988）
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,988,20,20},{297,988,20,20},{459,988,20,20}}
#elif defined (MX2116_NF_S5062)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,20,20},{408,884,20,20}}
#elif defined (MX2116_NF_S5062_QHD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,990,20,20},{297,990,20,20},{459,990,20,20}}
#elif defined (MX2116_JC_C11)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{264,884,20,20},{408,884,20,20}}
#elif defined (MX2116_JC_C12)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,20,20},{360,884,20,20}}
#elif defined (MX2116_JC_C15)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,20,20},{360,884,20,20}}
#elif defined (MX2109F_HDS_X1508)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,1400,20,20},{297,1400,20,20},{459,1400,20,20}} //1400
#elif defined (MX2111_WPF_D5020)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}} //1400
#elif defined(MX2109F_WPF_D5018)	//72 880 216 880 408 880
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,880,20,20},{216,880,20,20},{408,880,20,20}}
#elif defined(MX2116_XHT_771)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{80,900,60,60},{240,900,60,60},{400,900,60,60}}
#elif defined (MX2116_BER_S6001)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}} //1400
#elif defined(MX2116_HYF_JD3)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{264,884,20,20},{408,884,20,20}}
#elif defined (MX2116_ARES_A5083B_QHD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,990,20,20},{243,990,20,20},{459,990,20,20}}
#elif defined (MX2116G_BER_S5088_FW)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{408,884,20,20}}
#elif defined (MX2117_WPF_D5022B)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{408,884,20,20}}
#elif defined (MX2116G_BER_S4523)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{264,884,30,30},{408,884,20,20}}
#elif defined (MX2116G_JY_A5009)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,20,20},{408,884,20,20}}
#elif defined(MX2116_MJ_6001)	
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_BACK,KEY_HOMEPAGE,KEY_MENU}
#define TPD_KEYS_DIM            {{408,880,20,20},{216,880,20,20},{72,880,20,20}}
#elif defined (MX2117_ARES_A5029)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{408,884,20,20}}
#elif defined (MX2126E_ARES_A5032)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{408,884,20,20}}
#elif defined (MX1091G_JC_C18)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,20,20},{360,884,20,20}}
#elif defined (MX2126E_JC_C19_FW)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,20,20},{360,884,20,20}}
#elif defined (MX2126E_JC_R19_HD720)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}}
#elif defined (MX2126E_JC_C191_HD720)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}}
#elif defined (MX2118_GLT_G92_QHD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,990,20,20},{243,990,20,20},{459,990,20,20}}
#elif defined (MX2111_BEF_S5516)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}} //1400
#elif defined (MX2111E_WPF_D5020E_HD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}} //1400
#elif defined (MX2120E_BER_5065_HD)||defined (MX2120E_BER_5065_HD_YZ)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}} //1400
#elif defined (MX1091E_ARES_A5509_HD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1317,20,20},{396,1317,20,20},{612,1317,20,20}}
#elif defined (VH2521E_BANGHUA_U12)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,50,30},{324,1320,50,30},{612,1320,50,30}}
#elif defined (MX1091E_ARES_A5509_QHD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,988,20,20},{297,988,20,20},{459,988,20,20}}
#elif defined (MX2111_BEF_S5070_FWVGA)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{408,884,20,20}}
#elif defined(MX1091E_BER_S5067_FW)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{408,884,20,20}}
#elif defined (MX2116G_FD_VE5056B_HD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{396,1320,40,40},{612,1320,20,20}} 
#elif defined (MX2126E_WPF_D5027_HD720)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{100,1500,20,20},{200,1500,20,20},{300,1500,20,20}} 
#elif defined (MX2126E_WPF_D5027_FWVGA)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{100,1500,20,20},{200,1500,20,20},{300,1500,20,20}} 
#elif defined (MX2126E_YAAO_X15_FW)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{408,884,20,20}}
#elif defined (MX2126E_YAAO_X15_HD720)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}}
#elif defined (MX1091E_WPF_D5512_QHD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{100,1000,20,20},{200,1000,30,30},{300,1000,20,20}}
#elif defined (MX2118_HYF_JD1)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,20,20},{360,884,20,20}}
#elif defined (MX2132E_BER_S6002_HD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}} //1400
#elif defined (MX2521E_JS2521_HD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}} //1400
#elif defined (MX2128E_JD_JD5002_HD720)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}}
#elif defined (MX2118_BER_S5071)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,20,20},{408,884,20,20}}
#elif defined (MX2116G_BER_S4553E_FW)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{216,884,30,30},{408,884,20,20}}
#elif defined (MX2132E_BER_S5567_HD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}} //1400
#elif defined (MX2132E_BER_S5068_HD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}}
#elif defined (MX2116E_HYF_9800_HD)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
//#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}}
#define TPD_KEYS_DIM            {{108,1317,20,20},{396,1317,30,30},{612,1317,20,20}}
#elif defined (MX2132N_WPF_D5516)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}}
#elif defined (MX1091E_BER_S5588)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}}
#elif defined (MX2120E_HDS_S702)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,988,20,20},{243,988,20,20},{459,988,20,20}}
#elif defined (MX2120E_HDS_S703)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{108,1320,20,20},{324,1320,20,20},{612,1320,20,20}}
#elif defined (MX2120E_HDS_S703B)
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{81,988,20,20},{243,988,20,20},{459,988,20,20}}
#else
#define TPD_KEY_COUNT           3
#define TPD_KEYS                {KEY_MENU,KEY_HOMEPAGE,KEY_BACK}
#define TPD_KEYS_DIM            {{72,884,20,20},{264,884,20,20},{408,884,20,20}}
#endif


#ifdef GSL_ALG_ID 
extern unsigned int gsl_mask_tiaoping(void);
extern unsigned int gsl_version_id(void);
extern void gsl_alg_id_main(struct gsl_touch_info *cinfo);
extern void gsl_DataInit(int *ret);


#endif
/* Fixme mem Alig */
struct fw_data
{
    u32 offset : 8;
    u32 : 0;
    u32 val;
};
#ifdef GSL_DRV_WIRE_IDT_TP
#include "gsl_idt_tp.h"
#endif

#if   defined (MX1092_ARES_5091)
#include "gsl_ts_mx1092_ares_5091_fw.h"
#elif defined (MX2117L_WPF_D5015E_FWVGA)
#include "gsl_ts_mx2117_wpf_d5015e_fw_hsd.h"
#include "gsl_ts_mx2117_wpf_d5015e_fw_mwd.h"
#elif defined (MX2118_BER_S5071)
#include "gsl_ts_mx2118_ber_s5071_fw_mwd.h"
#include "gsl_ts_mx2118_ber_s5071_fw_xyd.h"
#elif defined (MX2118B_BER_S5071K)
#include "gsl_ts_mx2118_ber_s5071_fw_mwd.h"
#include "gsl_ts_mx2118_ber_s5071_fw_xyd.h"
#elif defined (MX1092_HYF_V7)
#include "gsl_ts_mx1092_hyf_v7_fw.h"
#elif defined (VH2521_A5590)
#include "gsl_ts_a5590.h"
#elif defined (VH2521_BANGHUA_D1C)
#include "gsl_ts_vh2521_d1_hd720_mingwangda.h"
#elif defined (VH2521E_JC_R21)
#include "gsl_ts_vh2521e_r21_qhd_dalong.h"
#elif defined (MX2118B_JD_HT5001K)
#include "gsl_ts_mx2118b_jd_ht5001k_hd_gsl1691_mwd.h"
#elif defined (VH2521E_ALS_A5039_FW)
#include "gsl_ts_vh2521e_als_a5039_fw_mwd.h"
#include "gsl_ts_vh2521e_als_a5039_fw_hsd.h"
#elif defined (VH2521E_ALS_A5039_FW_FOR_HSD)
#include "gsl_ts_vh2521e_als_a5039_fw_hsd_one.h"
#elif defined (MX1092_HYF_A1_FWVGA)
#include "gsl_ts_fw_hyf_a1.h"
#elif defined (MX1092_JY_A5006)
#include "gsl_ts_mx1092_jy_a5006.h"
#elif defined (MX1091_HYF_9300)
#include "gsl_ts_mx1091_hyf_9300_fw.h"
#elif defined (MX1093_GL4510)
#include "gsl_ts_mx1093_gl4510_fw.h"
#elif defined(MX1091_HYF_A1_PRO)
#include "gsl_ts_mx1091_hyf_a1_pro.h"
#elif defined (MX1091_ARES_A5088)
#include "gsl_ts_mx1091_ares_a5088.h"
#elif defined (MX1091_ARES_A5088_FW)
#include "gsl_ts_mx1091_ares_a5088_fw.h"
#elif defined (MX2116_SOP_TN_TP_PROXIMITY)
#include "gsl_ts_mx2116_sop_n1_4g_low_hz_fw.h"
#elif defined (MX1091_ARES_A5019)
#include "gsl_ts_mx1091_ares_a5019.h"
#elif defined (MX1091_ARES_A5019_FW)
#include "gsl_ts_mx1091_ares_a5019_fw.h"
#elif defined(MX1091_ARES_A5502)
#include "gsl_ts_mx1091_ares_a5502.h"
#elif defined (VH2510_WPF_D5013_FW)
#include "gsl_ts_vh2510_wpf_d5013_fw.h"
#elif defined (VH2510_WPF_D5016)
#include "gsl_ts_vh2510_wpf_d5016.h"
#elif defined (VH2510_NUOFEI_S5042B)
#include "gsl_ts_vh2510_wpf_s5042b.h"
#elif defined (MX2116_SMT_FW)
#include "gsl_ts_mx2116_smt_fw.h"
#elif defined (MX2118B_SMT_FW)
#include "gsl_ts_mx2116_smt_fw.h"
#elif defined (MX1098_SMT_FW)
#include "gsl_ts_mx2116_smt_fw.h"
#elif defined (MX2103_SMT_FW)
#include "gsl_ts_mx2116_smt_fw.h"
#elif defined (MX2116_ARES_A5086_FW)
#include "gsl_ts_mx2116_ares_a5086_fw.h"
#elif defined (MX2116_ARES_A5086_QHD)
#include "gsl_ts_mx2116_ares_a5086_qhd.h"
#elif defined (MX2116_WPF_D5015)
//#include "gsl_ts_mx2116_wpf_d5015_fw_mwd.h"
//#include "gsl_ts_mx2116_wpf_d5015_fw_hsd.h"
//#include "gsl_ts_mx2116_wpf_d5015_fw_mwd_gsl_1691.h"
#include "gsl_ts_mx2116_wpf_d5015_fw_mwd_gsl1691_hsd_gsl1691.h"
#elif defined (MX2116_WPF_D5015_TEST)
#include "gsl_ts_mx2116_wpf_d5015_fw_mwd.h"
#elif defined (MX2116_WPF_D5019)
#include "gsl_ts_mx2116_wpf_d5019_fw.h" 
#elif defined (MX2116_BER_S4513)
#include "gsl_ts_mx2116_ber_s4513_fw.h"
#elif defined (MX2116_BER_S4513_QHD)
#include "gsl_ts_mx2116_ber_s4513_qhd.h"
#elif defined (MX2116_BER_S5075)
#include "gsl_ts_mx2116_ber_s5075_fw.h"
#elif defined (MX2116_WPF_D5012)
#include "gsl_ts_mx2116_wpf_d5012.h"
#elif defined (MX2116_WPF_D4513)
#include "gsl_ts_mx2116_wpf_d4513_fw.h"
#elif defined (MX2116_WPF_D4513_QHD)
#include "gsl_ts_mx2116_wpf_d4513_qhd.h"
#elif defined (MX2116_FD_VE4518)
#include "gsl_ts_mx2116_fd_ve4518_fw.h"
#elif defined (MX2116_BER_S5513)
#include "gsl_ts_mx2116_ber_s5513_fw.h"
#elif defined (MX2116G_FD_VE5056B_HD)
#include "gsl_ts_mx2116g_fd_ve5056b_hd_mwd.h"
#include "gsl_ts_mx2116g_fd_ve5056b_hd_lq.h"
#elif defined (MX2109F_ARES_5503)
#include "gsl_ts_mx2109f_ares_5503_fw.h"
#elif defined (MX2109F_ARES_A5085)||defined(MX2109F_ARES_A5085_FOR_MX2132ER)
#include "gsl_ts_mx2109f_ares_a5085_fw.h"
#elif defined (MX2109F_ARES_A5085_QHD)
#include "gsl_ts_mx2109f_ares_a5085_qhd.h"
#elif defined (MX2116_NF_S5051)
#include "gsl_ts_mx2116_nf_s5051.h"
#elif defined (MX2116_NF_S5063_CTA)
#include "gsl_ts_mx2116_nf_s5063_fw.h"
//#elif 0//defined (MX2118_NF_S5063)
//#include "gsl_ts_mx2118_nf_s5063_fw.h"
#elif defined (MX2118_NF_S5063)
#include "gsl_ts_mx2116_nf_s5063_fw_968_hxwc.h"
#include "gsl_ts_mx2116_nf_s5063_fw_1691_mwd.h"
#include "gsl_ts_mx2116_nf_s5063_fw_1691_kll.h"
#elif defined (MX2116_NF_S5559)
#include "gsl_ts_mx2116_nf_s5559_fw.h"
#elif defined (MX1092_JC_C03_FWVGA)
#include "gsl_ts_mx1092_jc_c03_fw.h"
#elif defined (MX2103_JC_C09_FWVGA)
#include "gsl_ts_mx2103_jc_c09_fw.h"
#elif defined (MX2116_NF_S5062)
#include "gsl_ts_mx2116_nf_s5062.h"
#elif defined (MX2109F_HDS_X1508)
#include "gsl_ts_mx2109f_hds_x1508_fw.h"
#elif defined (MX2111_WPF_D5020)
#include "gsl_ts_mx2111_wpf_d5020_hd720.h"
#elif defined (MX2116_NF_S5062_QHD)
#include "gsl_ts_mx2116_nf_s5062_qhd.h"
#elif defined (MX2116_JC_C11)
#include "gsl_ts_mx2116_jc_c113.h"
#elif defined (MX2116_JC_C12)
//#include "gsl_ts_mx2116_jc_c12.h"
#include "gsl_ts_mx2116_jc_c12_fw_mwd.h"
#include "gsl_ts_mx2116_jc_c12_fw_yc.h"
#elif defined (MX2109F_WPF_D5018)
#include "gsl_ts_mx2109f_wpf_d5018_fw.h"
#elif defined (MX2116_XHT_771)
#include "gsl_ts_mx2116_xht_771_fw.h"
#elif defined (MX2116_BER_S6001)
#include "gsl_ts_mx2116_ber_s6001_hd720_fw.h"
#elif defined (MX2116_HYF_JD3)
#include "gsl_ts_mx2116_hyf_jd3_fw.h"
#elif defined (MX2116_ARES_A5083B_QHD)
#include "gsl_ts_mx2116_ares_a5083b_qhd.h"
#elif defined (MX2116G_BER_S5088_FW)
#include "gsl_ts_mx2116g_ber_s5088_fw_hfz.h"
#include "gsl_ts_mx2116g_ber_s5088_fw_hsd.h"
#include "gsl_ts_mx2116g_ber_s5088_fw_mwd.h"
#elif defined (MX2116G_WPF_D5023_HD720)
#include "gsl_ts_mx2116g_wpf_d5023_hd720_mwd_gsl1691.h"
#include "gsl_ts_mx2116g_wpf_d5023_hd720_hsd_gsl1691.h"
#elif defined (MX2117_WPF_D5022B)
#include "gsl_ts_mx2117_wpf_d5022b_fw_hsd.h"
#include "gsl_ts_mx2117_wpf_d5022b_fw_mwd.h"
#include "gsl_ts_mx2117_wpf_d5022b_fw_xyd.h"
#elif defined (MX2116G_BER_S4523)
#include "gsl_ts_mx2116g_ber_s4523_fw_mwd_gsl1691.h"
#elif defined (MX2116G_JY_A5009)
#include "gsl_ts_mx2116g_jy_a5009_fw_mwd_gsl_1691.h"
#elif defined (MX2116_MJ_6001)
#include "gsl_ts_mx2116_mj_6001_fw.h"
#elif defined (MX2116G_FD_VE5056)
//#include "gsl_ts_mx2116g_fd_ve5056_fw_linqi_gsl1691.h"
#include "gsl_ts_mx2116g_fd_ve5056_fw.h"
#elif defined (MX2116G_BER_S5089_HD720)
#include "gsl_ts_mx2116g_ber_s5089_hd720_hfz.h"
#include "gsl_ts_mx2116g_ber_s5089_hd720_hsd.h"
#include "gsl_ts_mx2116g_ber_s5089_hd720_mwd.h"
#elif defined (MX2109E_WPF_D5018D_HD720)
#include "gsl_ts_mx2109e_wpf_d5018d_hd720_mwd_gsl1691.h"
#elif defined (MX2117_ARES_A5029)
#include "gsl_ts_mx2117_ares_a5029_fw_mwd.h"
#include "gsl_ts_mx2117_ares_a5029_fw_hsd.h"
#elif defined (MX2126E_ARES_A5032)
#include "gsl_ts_mx2126e_ares_a5032_fw_mwd.h"
#include "gsl_ts_mx2126e_ares_a5032_fw_hsd.h"
#elif defined (MX2116_JC_C15)
#include "gsl_ts_mx2116_jc_c15.h"
#include "gsl_ts_hsd_mx2116_jc_c15.h"
#elif defined (MX1091G_JC_C18)
#include "gsl_ts_mx1091g_jc_c18_fw_gsl1691_mwd.h"
#elif defined (MX2126E_BER_S5518_QHD)
//#include "gsl_ts_mx2126_ber_s5518_qhd_gsl968.h"
#include "gsl_ts_mx2126e_ber_s5518_qhd_gsl1691_mwd_xyd.h"
#elif defined (MX2126E_WPF_D5027_HD720)
#include "gsl_ts_mx2126e_wpf_d5027_hd720_gsl1691_mwd.h"
#include "gsl_ts_mx2126e_wpf_d5027_hd720_gsl1691_hsd.h"
#elif defined (MX2126E_WPF_D5027_FWVGA)
//include "gsl_ts_mx2126e_wpf_d5027_fwvga_gsl1691_mwd.h"
#include "gsl_ts_mx2126e_wpf_d5027_fwvga_gsl1691_hsd.h"
#elif defined (MX2126E_JC_C19_FW)
#include "gsl_ts_mx2126e_jc_c19_fw.h"
//#include "gsl_ts_mx1091g_jc_c18_fw_gsl1691_mwd.h"
#elif defined (MX2126E_JC_R19_HD720)
#include "gsl_ts_mx2126e_jc_r19_hd720.h"
#elif defined (MX2126E_JC_C191_HD720)
#include "gsl_ts_mx2126e_jc_c191_hd720.h"
#elif defined (MX2111_BEF_S5070_HD720)
#include "gsl_ts_mx2111_bef_s5070_hd720_hfz.h"
#include "gsl_ts_mx2111_bef_s5070_hd720_mwd.h"
#elif defined (MX1091E_BER_S5067_FW)
#include "gsl_ts_mx1091e_ber_s5067_fw_mwd.h"
#include "gsl_ts_mx1091e_ber_s5067_fw_hsd.h"
#elif defined (MX2111_BEF_S5516)
#include "gsl_ts_mx2111_bef_s5516_hd720.h"
#elif defined (MX2118_GLT_G92_QHD)
#include "gsl_ts_mx2118_glt_g92_qhd.h"
#elif defined (MX2118_JC_QHD_C116)
#include "gsl_ts_mx2118_jc_qhd_c116.h"
//#elif defined (MX1091E_WPF_D5028_FW)
//#include "gsl_ts_mx1091e_wpf_d5028_fw_mwd.h"
#elif defined (MX2111E_WPF_D5020E_HD)
#include "gsl_ts_mx2111e_wpf_d5020e_hd720_mwd_960.h"
#include "gsl_ts_mx2111e_wpf_d5020e_hd720_mwd_1691.h"
#elif defined (MX1091E_ARES_A5509_HD)
#include "gsl_ts_mx1091e_ares_a5509_hd720_mwd_1691.h"
#include "gsl_ts_mx1091e_ares_a5509_hd720_hsd_1691.h"
#elif defined (VH2521E_BANGHUA_U12)
#include "gsl_ts_vh2521e_banghua_u12_hd720_1691.h"
#elif defined (MX1091E_ARES_A5509_QHD)
#include "gsl_ts_mx1091e_ares_a5509_qhd_mwd_1691.h"
#include "gsl_ts_mx1091e_ares_a5509_qhd_hsd_1691.h"
#elif defined (MX2120E_BER_5065_HD)
#include "gsl_ts_mx2120e_ber_5065_hd_1691_mwd.h"
#elif defined (MX2120E_BER_5065_HD_YZ)
#include "gsl_ts_mx2120e_ber_5065_hd_1691_mwd_plus_sensitivity.h"
#elif defined (MX2120E_BER_S5566_HD)
#include "gsl_ts_mx2120e_ber_s5566_hd.h"
#elif defined (MX2120E_WPF_D5513_HD)
#include "gsl_ts_mx2120e_wpf_d5513_hd_mwd.h"
#include "gsl_ts_mx2120e_wpf_d5513_hd_hsd.h"
#elif defined (MX2128E_ALS_A5066_FWVGA)
#include "gsl_ts_mx2128e_als_a5066_fwvga_mwd.h"
#include "gsl_ts_mx2128e_als_a5066_fwvga_hsd.h"
#elif defined (MX2128E_ALS_A5066_FWVGA_ONE)
#include "gsl_ts_mx2128e_als_a5066_fwvga_hsd_one.h"
#elif defined (MX1091E_WPF_D5512_QHD)
#include "gsl_ts_mx1091e_wpf_d5512_qhd_gsl915_mwd.h"
#include "gsl_ts_mx1091e_wpf_d5512_qhd_gsl915_hsd.h"
#elif defined (MX1091E_WPF_D5512I_HD720)
#include "gsl_ts_mx1091e_wpf_d5512i_hd720_gsl915_mwd.h"
#elif defined (MX1091E_WPF_D5512_HD720)
#include "gsl_ts_mx1091e_wpf_d5512_hd720_gsl915_mwd.h"
#include "gsl_ts_mx1091e_wpf_d5512_hd720_gsl915_hsd.h"
#elif defined (MX2111_BEF_S5070_FWVGA)
#include "gsl_ts_mx2111_bef_s5070_fwvga_hfz.h"
#include "gsl_ts_mx2111_bef_s5070_fwvga_mwd.h"
#elif defined (MX2111_BEF_S5537)
#include "gsl_ts_mx2111_bef_s5537_hd720.h"
#elif defined (MX2126E_YAAO_X15_FW)
#include "gsl_ts_mx2126e_yaao_x15_fw_mwd_gsl1691.h"
#elif defined (MX2126E_YAAO_X15_HD720)
#include "gsl_ts_mx2126e_yaao_x15_hd720_mwd_gsl1691.h"
#elif defined (MX2118_HYF_JD1)
#include "gsl_ts_mx2118_hyf_jd_fw.h"
#elif defined (MX1091E_WPF_D5028_FW)
#include "gsl_ts_mx1091e_wpf_d5028_fw_mwd.h"
#include "gsl_ts_mx1091e_wpf_d5028_fw_hsd.h"
#elif defined (MX1091E_WPF_D5028B_HD)
#include "gsl_ts_mx1091e_wpf_d5028b_hd_gsl1691_mwd.h"
#include "gsl_ts_mx1091e_wpf_d5028b_hd_gsl1691_hsd.h"
#elif defined (MX2132E_BER_S6002_HD)
#include "gsl_ts_mx2132e_ber_s6002_hd_gsl915_hsd.h"
#elif defined (MX2521E_JS2521_HD)
#include "gsl_ts_mx2521e_js2521_hd_gsl915_mwd.h"
#elif defined (MX2128N_BER_S5056_HD720)
#include "gsl_ts_mx2128n_ber_s5056_hd720_gsl1691_hsd.h"
#elif defined (MX2128E_JD_JD5002_HD720)
#include "gsl_ts_mx2128e_jd_jd5002_hd720_gsl1691.h"
#elif defined (MX2128E_WPF_D5035_FWVGA)
#include "gsl_ts_mx2128e_wpf_d5035_fwvga_gsl1691.h"
#elif defined (MX2128E_WPF_D5031_FWVGA)
#include "gsl_ts_mx2128e_wpf_d5031_fwvga_gsl1691_mwd.h"
#include "gsl_ts_mx2128e_wpf_d5031_fwvga_gsl1691_hsd.h"
#elif defined (MX2128E_WPF_D5030B_FWVGA)
#include "gsl_ts_mx2128e_wpf_d5030b_fwvga_gsl1691_mwd.h"
#include "gsl_ts_mx2128e_wpf_d5030b_fwvga_gsl1691_hsd.h"
#elif defined (MX2150E_WPF_D5033_FWVGA)
#include "gsl_ts_mx2150e_wpf_d5033_fwvga_gsl1691_mwd.h"
#include "gsl_ts_mx2150e_wpf_d5033_fwvga_gsl1691_hsd.h"
#elif defined (MX2128E_WPF_D5030_HD720)
#include "gsl_ts_mx2128e_wpf_d5030_hd720_gsl1691_mwd.h"
#include "gsl_ts_mx2128e_wpf_d5030_hd720_gsl1691_hsd.h"
#elif defined (MX2116G_BER_S4553E_FW)
#include "gsl_ts_mx2116g_ber_s4553e_fw_1691.h"
#elif defined (MX2132E_BER_S5567_HD)
#include "gsl_ts_mx2132e_ber_s5567_hd_gsl915_mwd.h"
#elif defined (MX2132E_BER_S5068_HD)
#include "gsl_ts_mx2132e_ber_s5068_hd_gsl1691_mwd.h"
#elif defined (MX2116E_HYF_9800)
#include "gsl_ts_mx2116e_hyf_9800.h"
#elif defined (MX2116E_HYF_9800_HD)
#include "gsl_ts_mx2116e_hyf_9800_hd.h"
#elif defined (MX2116E_JC_C16)
#include "gsl_ts_mx2116e_jc_c16_fw.h"
#elif defined (MX2116E_JC_V152)
#include "gsl_ts_mx2116e_jc_v152_hd.h"
#elif defined (MX2132N_WPF_D5516)
#include "gsl_ts_mx2132n_wpf_d5516_hd_mwd.h"
#elif defined (MX1091E_BER_S5588)
#include "gsl_ts_mx1091e_ber_s5588_hd_mwd.h"
#elif defined (MX2120E_HDS_S702)
#include "gsl_ts_mx2120e_hds_s702_qhd_mwd.h"
#elif defined (MX2120E_HDS_S703)
#include "gsl_ts_mx2120e_hds_s703_hd_mwd.h"
#elif defined (MX2120E_HDS_S703B)
#include "gsl_ts_mx2120e_hds_s703b_qhd_mwd.h"
#elif defined (MX2521E_WPF_D5517)
#include "gsl_ts_mx2521e_wpf_d5517_hd720_gsl1691_mwd.h"
#include "gsl_ts_mx2521e_wpf_d5517_hd720_gsl1691_hsd.h"
#elif defined (MX2150E_JY_A5010_FWVGA)
#include "gsl_ts_mx2150e_jy_a5010_fwvga_gsl1961_mwd.h"
#elif defined (MX2521E_WPF_D5036_HD)
#include "gsl_ts_mx2521e_wpf_d5036_hd720_gsl1691_mwd.h"
#elif defined (MX2521E_YAAO_X27_HD)
#include "gsl_ts_mx2521e_yaao_x27_hd720_gsl1691_mwd.h"
#else
#include "gsl_ts_fw.h"
#endif

#ifdef GSL_DRV_WIRE_IDT_TP
#if defined (MX2116_WPF_D5015)
#define GSL_C		100
#define GSL_CHIP_1	0xff807801 //HSD
#define GSL_CHIP_2	0xff80f801  //MWD
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2117L_WPF_D5015E_FWVGA)
#define GSL_C		100
#define GSL_CHIP_1	0xff80f801  //MWD
#define GSL_CHIP_2	0xff807801 //HSD
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2116G_BER_S5088_FW)
#define GSL_C		100
#define GSL_CHIP_1	0xffa07801 //HFZ
#define GSL_CHIP_2	0xff907a01  //HSD
#define GSL_CHIP_3	0xffc07801   //MWD
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2116G_FD_VE5056)
#define GSL_C		100
//#define GSL_CHIP_1	0xffa07801 //HFZ
#define GSL_CHIP_1	0xff807803  //QILIN
#define GSL_CHIP_2	0xffc07801   //MWD
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined(MX2117_WPF_D5022B)
#define GSL_C		100
#define GSL_CHIP_1	0xff807803 //hsd  ff807801
#define GSL_CHIP_2	0xff81f801 //mwd  ff81f801
#define GSL_CHIP_3	0xffc07801 //xyd 0xffc07801
#elif defined(MX2116G_WPF_D5023_HD720)
#define GSL_C		100
#define GSL_CHIP_1	0xff807803 //mwd    ff807803
#define GSL_CHIP_2	0xffc07881 //hsd    ffc07881
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff 
#elif defined (MX1091E_BER_S5067_FW)
#define GSL_C		100
#define GSL_CHIP_1	0xffc07803 //mwd    ff807803
#define GSL_CHIP_2	0xff907801 //hsd    ffc07881
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined(MX2116_JC_C12)
#define GSL_C		100
#define GSL_CHIP_1	0xffc07801  //mwd
#define GSL_CHIP_2	0xff807807  //yc
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2116G_BER_S5089_HD720)
#define GSL_C		100
#define GSL_CHIP_1	0xffa07801 //HFZ
#define GSL_CHIP_2	0xff907a01  //HSD
#define GSL_CHIP_3	0xffc07801   //MWD
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2117_ARES_A5029)
#define GSL_C		100
#define GSL_CHIP_1	0xff807803 //HSD
#define GSL_CHIP_2	0xff80f801  //MWD
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined(MX2126E_ARES_A5032)
#define GSL_C		100
#define GSL_CHIP_1	0xff807803 //HSD
#define GSL_CHIP_2	0xff80f801  //MWD
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined(MX2118_NF_S5063)
#define GSL_C		100
#define GSL_CHIP_1	0xff80f801 //1691 mwd
#define GSL_CHIP_2	0xffc8a500 //968 hxwc
#define GSL_CHIP_3	0xff807803 //1691  kll
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2111_BEF_S5070_HD720)
#define GSL_C		100
#define GSL_CHIP_1	0xffa07801 //HFZ
#define GSL_CHIP_2	0xffc07801   //MWD
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined(MX2111E_WPF_D5020E_HD)
#define GSL_C		100
#define GSL_CHIP_1	0xff80f801 //1691 mwd
#define GSL_CHIP_2	0xffc8a401 //960
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2111_BEF_S5070_FWVGA)
#define GSL_C		100
#define GSL_CHIP_1	0xffa07801 //HFZ
#define GSL_CHIP_2	0xffc07801   //MWD
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX1091E_WPF_D5512_QHD)
#define GSL_C		100
#define GSL_CHIP_1	0xfff00000 //hsd
#define GSL_CHIP_2	0xffe00000   //MWD
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2120E_WPF_D5513_HD)
#define GSL_C		100
#define GSL_CHIP_1	0xfff00000 //hsd
#define GSL_CHIP_2	0xffe00000   //MWD
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2128E_ALS_A5066_FWVGA)
#define GSL_C		100
#define GSL_CHIP_1	0xff807c01 //mwd
#define GSL_CHIP_2	0xff807801 //hsd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX1091E_WPF_D5512_HD720)
#define GSL_C		100
#define GSL_CHIP_1	0xfff00000 //hsd
#define GSL_CHIP_2	0xffe00000   //MWD
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX1091E_ARES_A5509_HD)
#define GSL_C		100
#define GSL_CHIP_1	0xffc07801 //mwd
#define GSL_CHIP_2	0xff807803   //hsd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined(MX2116_JC_C15)
#define GSL_C		100
#define GSL_CHIP_1	0xffc07801 //mwd
#define GSL_CHIP_2	0xff807801   //hsd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX1091E_ARES_A5509_QHD)
#define GSL_C		100
#define GSL_CHIP_1	0xffc07801 //mwd
#define GSL_CHIP_2	0xff807803   //hsd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2116G_FD_VE5056B_HD)
#define GSL_C		100
#define GSL_CHIP_1	0xff807803 //MWD
#define GSL_CHIP_2	0xffc07801   //lq
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined(MX1091E_WPF_D5028_FW)
#define GSL_C		100
#define GSL_CHIP_1	0xff80f801 //mwd
#define GSL_CHIP_2	0xff807c01 //hsd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined(MX1091E_WPF_D5028B_HD)
#define GSL_C		100
#define GSL_CHIP_1	0xff80f801 //mwd  ff80f801 
#define GSL_CHIP_2	0xff807c01 //hsd  ff807c01
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined(MX2118_BER_S5071)
#define GSL_C		100
#define GSL_CHIP_1	0xffeaa500 //mwd
#define GSL_CHIP_2	0xff80f801 //xyd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined(MX2118B_BER_S5071K)
#define GSL_C		100
#define GSL_CHIP_1	0xffeaa500 //mwd
#define GSL_CHIP_2	0xff80f801 //xyd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2128E_WPF_D5031_FWVGA)
#define GSL_C		100
#define GSL_CHIP_1	0xff80f801 //mwd
#define GSL_CHIP_2	0xff807c01 //hsd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2128E_WPF_D5030B_FWVGA)
#define GSL_C		100
#define GSL_CHIP_1	0xffc07801 //mwd
#define GSL_CHIP_2	0xff807805 //hsd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2150E_WPF_D5033_FWVGA)
#define GSL_C		100
#define GSL_CHIP_1	0xff80f801 //mwd
#define GSL_CHIP_2	0xff807803 //hsd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (MX2128E_WPF_D5030_HD720)
#define GSL_C		100
#define GSL_CHIP_1	0xffc07801 //mwd
#define GSL_CHIP_2	0xff807805 //hsd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined (VH2521E_ALS_A5039_FW)
#define GSL_C		100
#define GSL_CHIP_1	0xff80f803 //mwd
#define GSL_CHIP_2	0xffc07801 //hsd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#elif defined(MX2521E_WPF_D5517)
#define GSL_C		100
#define GSL_CHIP_1	0xffe00000 //mwd 
#define GSL_CHIP_2	0xffc00600 //hsd
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#else
#define GSL_C		100
#define GSL_CHIP_1	0xff807801 //HSD
#define GSL_CHIP_2	0xff80f801  //MWD
#define GSL_CHIP_3	0xffffffff
#define GSL_CHIP_4	0xffffffff
#endif
#endif

static unsigned char gsl_cfg_index = 0;

struct fw_config_type
{
	const struct fw_data *fw;
	unsigned int fw_size;
	unsigned int *data_id;
	unsigned int data_size;
};
static struct fw_config_type gsl_cfg_table[9] = {
#if defined( MX2116_WPF_D5015)
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
/*2*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
#elif defined(MX2117L_WPF_D5015E_FWVGA)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined (MX2126E_BER_S5518_QHD)
/*1*/{GSLX680_FW_XYD,(sizeof(GSLX680_FW_XYD)/sizeof(struct fw_data)),gsl_config_data_id_xyd,(sizeof(gsl_config_data_id_xyd)/4)},
/*2*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},

#elif defined (MX2116G_FD_VE5056)
/*1*/{GSLX680_FW_QILIN,(sizeof(GSLX680_FW_QILIN)/sizeof(struct fw_data)),gsl_config_data_id_qilin,(sizeof(gsl_config_data_id_qilin)/4)},
/*2*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},

#elif defined (MX2116G_BER_S5088_FW)
/*0*/{GSLX680_FW_HFZ,(sizeof(GSLX680_FW_HFZ)/sizeof(struct fw_data)),gsl_config_data_id_hfz,(sizeof(gsl_config_data_id_hfz)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
/*2*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
#elif defined (MX2117_WPF_D5022B)
/*0*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
/*1*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*2*/{GSLX680_FW_XYD,(sizeof(GSLX680_FW_XYD)/sizeof(struct fw_data)),gsl_config_data_id_xyd,(sizeof(gsl_config_data_id_xyd)/4)},
#elif defined(MX2116G_WPF_D5023_HD720)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined(MX1091E_BER_S5067_FW)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined(MX2116_JC_C12)
/*0*/{GSLX680_FW_MINGWANGDA,(sizeof(GSLX680_FW_MINGWANGDA)/sizeof(struct fw_data)),gsl_config_data_id_mingwangda,(sizeof(gsl_config_data_id_mingwangda)/4)},
/*1*/{GSLX680_FW_YUECHEN,(sizeof(GSLX680_FW_YUECHEN)/sizeof(struct fw_data)),gsl_config_data_id_yuechen,(sizeof(gsl_config_data_id_yuechen)/4)},
#elif defined (MX2116G_BER_S5089_HD720)
/*0*/{GSLX680_FW_HFZ,(sizeof(GSLX680_FW_HFZ)/sizeof(struct fw_data)),gsl_config_data_id_hfz,(sizeof(gsl_config_data_id_hfz)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
/*2*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
#elif defined(MX2117_ARES_A5029)
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
/*2*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
#elif defined(MX2126E_ARES_A5032)
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
/*2*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
#elif defined(MX2118_NF_S5063)
/*0*/{GSLX680_FW_1691,(sizeof(GSLX680_FW_1691)/sizeof(struct fw_data)),gsl_config_data_id_1691,(sizeof(gsl_config_data_id_1691)/4)},
/*1*/{GSLX680_FW_960,(sizeof(GSLX680_FW_960)/sizeof(struct fw_data)),gsl_config_data_id_960,(sizeof(gsl_config_data_id_960)/4)},
/*2*/{GSLX680_FW_KLL,(sizeof(GSLX680_FW_KLL)/sizeof(struct fw_data)),gsl_config_data_id_kll,(sizeof(gsl_config_data_id_kll)/4)},
#elif defined (MX2111_BEF_S5070_HD720)
/*0*/{GSLX680_FW_HFZ,(sizeof(GSLX680_FW_HFZ)/sizeof(struct fw_data)),gsl_config_data_id_hfz,(sizeof(gsl_config_data_id_hfz)/4)},
/*1*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
#elif defined (MX2111E_WPF_D5020E_HD)
/*0*/{GSLX680_FW_960,(sizeof(GSLX680_FW_960)/sizeof(struct fw_data)),gsl_config_data_id_960,(sizeof(gsl_config_data_id_960)/4)},
/*1*/{GSLX680_FW_1691,(sizeof(GSLX680_FW_1691)/sizeof(struct fw_data)),gsl_config_data_id_1691,(sizeof(gsl_config_data_id_1691)/4)},
#elif defined(MX2111_BEF_S5070_FWVGA)
/*0*/{GSLX680_FW_HFZ,(sizeof(GSLX680_FW_HFZ)/sizeof(struct fw_data)),gsl_config_data_id_hfz,(sizeof(gsl_config_data_id_hfz)/4)},
/*1*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
#elif defined (MX2126E_WPF_D5027_HD720)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined(MX2120E_WPF_D5513_HD)
/*0*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
/*1*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
#elif defined(MX2128E_ALS_A5066_FWVGA)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined(MX1091E_WPF_D5512_QHD)
/*0*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
/*1*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
#elif defined (MX1091E_ARES_A5509_HD)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined (MX2116_JC_C15)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined (MX1091E_ARES_A5509_QHD)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined(MX1091E_WPF_D5512_HD720)
/*0*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
/*1*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
#elif defined (MX2116G_FD_VE5056B_HD)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_LQ,(sizeof(GSLX680_FW_LQ)/sizeof(struct fw_data)),gsl_config_data_id_lq,(sizeof(gsl_config_data_id_lq)/4)},
#elif defined (MX1091E_WPF_D5028_FW)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined (MX1091E_WPF_D5028B_HD)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined (MX2118_BER_S5071)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_XYD,(sizeof(GSLX680_FW_XYD)/sizeof(struct fw_data)),gsl_config_data_id_xyd,(sizeof(gsl_config_data_id_xyd)/4)},
#elif defined (MX2118B_BER_S5071K)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_XYD,(sizeof(GSLX680_FW_XYD)/sizeof(struct fw_data)),gsl_config_data_id_xyd,(sizeof(gsl_config_data_id_xyd)/4)},
#elif defined (MX2128E_WPF_D5031_FWVGA)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined (MX2128E_WPF_D5030B_FWVGA)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined (MX2150E_WPF_D5033_FWVGA)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined (MX2128E_WPF_D5030_HD720)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#elif defined (VH2521E_ALS_A5039_FW)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},

#elif defined (MX2521E_WPF_D5517)
/*0*/{GSLX680_FW_MWD,(sizeof(GSLX680_FW_MWD)/sizeof(struct fw_data)),gsl_config_data_id_mwd,(sizeof(gsl_config_data_id_mwd)/4)},
/*1*/{GSLX680_FW_HSD,(sizeof(GSLX680_FW_HSD)/sizeof(struct fw_data)),gsl_config_data_id_hsd,(sizeof(gsl_config_data_id_hsd)/4)},
#else
/*0*/{GSLX680_FW,(sizeof(GSLX680_FW)/sizeof(struct fw_data)),gsl_config_data_id,(sizeof(gsl_config_data_id)/4)},
#endif
};

#endif
