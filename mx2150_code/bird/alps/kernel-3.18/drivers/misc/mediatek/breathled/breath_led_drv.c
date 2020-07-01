/*
 * Copyright (C) 2010 MediaTek, Inc.
 * 
 * Author: Terry Chang <terry.chang@mediatek.com>
 *
 * This software is licensed under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation, and
 * may be copied, distributed, and modified under those terms.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */
#include <linux/module.h>
#include <linux/moduleparam.h>
#include <linux/init.h>
#include <linux/kernel.h>	/* BREATHLED_LOG() */
#include <linux/slab.h>		/* kmalloc() */
#include <linux/fs.h>		/* everything... filp_open*/
#include <linux/errno.h>	/* error codes */
#include <linux/types.h>	/* size_t */
#include <linux/proc_fs.h>  /*proc*/
#include <linux/fcntl.h>	/* O_ACCMODE */
#include <linux/aio.h>
#include <linux/uaccess.h>  /*set_fs get_fs mm_segment_t*/
#include <linux/miscdevice.h>
#include <linux/platform_device.h>
#include <linux/unistd.h>
#include <linux/cdev.h>
#include <linux/device.h>
#include <linux/mtd/mtd.h>
#include <linux/sched.h>	//show_stack(current,NULL)
//#include <mach/env.h>
#include <linux/string.h>
#include <linux/init.h>
#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/input.h>
#include <linux/workqueue.h>
#include <linux/timer.h>
#include <linux/interrupt.h>
#include <linux/fs.h>
#include <linux/miscdevice.h>
#include <linux/platform_device.h>
//#include <linux/earlysuspend.h>
#include <linux/delay.h>
#include <linux/ioctl.h>
#include <asm/atomic.h>
#include <linux/gpio.h>
//#include <asm/uaccess.h>

#if 0
#include <mach/mt_reg_base.h>
#include <mach/mt_boot.h>
#include <mtk_kpd.h>		/* custom file */
#include <mach/irqs.h>
#include <mach/eint.h>
#include <mach/mt_gpio.h>
#include <mach/mt_pmic_wrap.h>
#include <mach/mt_sleep.h>
#include <mach/mt_gpio_core.h>
#include <mach/sync_write.h>
#include <mach/mt_typedefs.h>
#endif

//#include <linux/aee.h>

#define breathled_log_on 0

#ifdef breathled_log_on
#define BREATHLED_TAG "[zxw]"
//#define BREATHLED_LOG(fmt)            printk(BREATHLED_TAG fmt)
#define BREATHLED_LOG(fmt,args...)    printk(BREATHLED_TAG fmt,##args)
#else
//#define BREATHLED_LOG(fmt)		   do{}while(0)
#define BREATHLED_LOG(fmt,args...)  do{}while(0)
#endif

#ifdef CONFIG_BIRD_BREATH_LED_SUPPORT_FOR_MX2151E
#define AW2013_SDA_PIN				67
#define AW2013_SCK_PIN				68
#else
#define AW2013_SDA_PIN				127
#define AW2013_SCK_PIN				128
#endif

#define IIC_ADDRESS_WRITE			0x8a       
#define IIC_ADDRESS_READ			0x8b       
#define AW2013_I2C_MAX_LOOP 		3 
#define I2C_delay 		3    //可根据平台调整,保证I2C速度不高于400k

//以下为调节呼吸效果的参数
#ifdef CONFIG_BIRD_BREATH_LED_LIGHTNESSCTRL_5MA
#define Imax          0x01
#else
#define Imax          0x02   //LED最大电流配置,0x00=omA,0x01=5mA,0x02=10mA,0x03=15mA,
#endif
#define Rise_time   0x02   //LED呼吸上升时间,0x00=0.13s,0x01=0.26s,0x02=0.52s,0x03=1.04s,0x04=2.08s,0x05=4.16s,0x06=8.32s,0x07=16.64s
#define Hold_time   0x01   //LED呼吸到最亮时的保持时间0x00=0.13s,0x01=0.26s,0x02=0.52s,0x03=1.04s,0x04=2.08s,0x05=4.16s
#define Fall_time     0x02   //LED呼吸下降时间,0x00=0.13s,0x01=0.26s,0x02=0.52s,0x03=1.04s,0x04=2.08s,0x05=4.16s,0x06=8.32s,0x07=16.64s
#define Off_time      0x01   //LED呼吸到灭时的保持时间0x00=0.13s,0x01=0.26s,0x02=0.52s,0x03=1.04s,0x04=2.08s,0x05=4.16s,0x06=8.32s,0x07=16.64s
#define Delay_time   0x00   //LED呼吸启动后的延迟时间0x00=0s,0x01=0.13s,0x02=0.26s,0x03=0.52s,0x04=1.04s,0x05=2.08s,0x06=4.16s,0x07=8.32s,0x08=16.64s
#define Period_Num  0x00   //LED呼吸次数0x00=无限次,0x01=1次,0x02=2次.....0x0f=15次  

/*
#define  AW2013_SCK_PIN_HIGH   mt_set_gpio_out_base(AW2013_SCK_PIN,1)
#define  AW2013_SCK_PIN_LOW    mt_set_gpio_out_base(AW2013_SCK_PIN,0)
#define  AW2013_SDA_PIN_HIGH   mt_set_gpio_out_base(AW2013_SDA_PIN,1)
#define  AW2013_SDA_PIN_LOW    mt_set_gpio_out_base(AW2013_SDA_PIN,0)
#define  AW2013_SDA_PIN_READ   mt_get_gpio_in_base(AW2013_SDA_PIN)
*/

unsigned char Paoma_cnt=0;
unsigned char Breath_cnt=0;
unsigned char cnt2=0;
static int global_flag=0;
/*****************************************************************************
Driver interface
*****************************************************************************/
#define BIRD_BREATHLED_DEVNAME            "breathled"
static struct class *bird_breathled_class = NULL;
static struct device *bird_breathled_device = NULL;

static dev_t bird_breathled_devno;
static struct cdev bird_breathled_cdev;
/****************************************************************************/


struct pinctrl *breahtlight_pinctrl;
struct pinctrl_state *breahledsda_high;
struct pinctrl_state *breahledsda_low;
struct pinctrl_state *breahledsck_high;
struct pinctrl_state *breahledsck_low;
struct pinctrl_state *breahledsda_in;

int breahtlight_gpio_init(struct platform_device *pdev)
{
	int ret = 0;

	breahtlight_pinctrl = devm_pinctrl_get(&pdev->dev);
	if (IS_ERR(breahtlight_pinctrl)) {
		ret = PTR_ERR(breahtlight_pinctrl);
		pr_debug("Cannot find breahtlight pinctrl!");
	}

	breahledsda_high = pinctrl_lookup_state(breahtlight_pinctrl, "breahledsda_high");
	if (IS_ERR(breahledsda_high)) {
		ret = PTR_ERR(breahledsda_high);
		pr_debug("%s : init err, breahledsda_high\n", __func__);
	}

	breahledsda_low = pinctrl_lookup_state(breahtlight_pinctrl, "breahledsda_low");
	if (IS_ERR(breahledsda_low)) {
		ret = PTR_ERR(breahledsda_low);
		pr_debug("%s : init err, breahledsda_low\n", __func__);
	}

	breahledsck_high = pinctrl_lookup_state(breahtlight_pinctrl, "breahledsck_high");
	if (IS_ERR(breahledsck_high)) {
		ret = PTR_ERR(breahledsck_high);
		pr_debug("%s : init err, breahledsck_high\n", __func__);
	}

	breahledsck_low = pinctrl_lookup_state(breahtlight_pinctrl, "breahledsck_low");
	if (IS_ERR(breahledsck_low)) {
		ret = PTR_ERR(breahledsck_low);
		pr_debug("%s : init err, breahledsck_low\n", __func__);
	}

	breahledsda_in = pinctrl_lookup_state(breahtlight_pinctrl, "breahledsda_in");
	if (IS_ERR(breahledsda_in)) {
		ret = PTR_ERR(breahledsda_in);
		pr_debug("%s : init err, breahledsda_in\n", __func__);
	}

	return ret;
}




void AW2013_delay_1us(unsigned short wTime) 
{
 if(wTime>1)
    udelay(wTime*2);
}

void AW2013_i2c_initial(void)
{
/*
	mt_set_gpio_mode_base(AW2013_SCK_PIN, 0);
	mt_set_gpio_dir_base( AW2013_SCK_PIN,1);
	AW2013_SCK_PIN_HIGH;
	
	mt_set_gpio_mode_base(AW2013_SDA_PIN, 0);
	mt_set_gpio_dir_base(AW2013_SDA_PIN,1);
	AW2013_SDA_PIN_LOW;
	AW2013_delay_1us(5);
	AW2013_SDA_PIN_HIGH;	
*/

	pinctrl_select_state(breahtlight_pinctrl, breahledsck_high);

	pinctrl_select_state(breahtlight_pinctrl, breahledsda_low);
	AW2013_delay_1us(5);
	pinctrl_select_state(breahtlight_pinctrl, breahledsda_high);

}



static void AW2013_i2c_start(void)
{
/*
	mt_set_gpio_dir_base(AW2013_SDA_PIN,1);
	mt_set_gpio_dir_base(AW2013_SCK_PIN,1);
	
	AW2013_SDA_PIN_HIGH;
	AW2013_SCK_PIN_HIGH;
	AW2013_delay_1us(2);
	AW2013_SDA_PIN_LOW;
	AW2013_delay_1us(2);	
	AW2013_SCK_PIN_LOW;
	AW2013_delay_1us(2);
*/	

	pinctrl_select_state(breahtlight_pinctrl, breahledsda_high);
	pinctrl_select_state(breahtlight_pinctrl, breahledsck_high);
	AW2013_delay_1us(2);
	pinctrl_select_state(breahtlight_pinctrl, breahledsda_low);
	AW2013_delay_1us(2);
	pinctrl_select_state(breahtlight_pinctrl, breahledsck_low);
	AW2013_delay_1us(2);
}

static void AW2013_i2c_stop(void)
{
/*
	mt_set_gpio_dir_base(AW2013_SDA_PIN,1);
	mt_set_gpio_dir_base(AW2013_SCK_PIN,1);	
	AW2013_SCK_PIN_LOW;
	AW2013_delay_1us(2);
	AW2013_SDA_PIN_LOW;
	AW2013_SCK_PIN_HIGH;
	AW2013_delay_1us(2);
	AW2013_SDA_PIN_HIGH;
*/
	pinctrl_select_state(breahtlight_pinctrl, breahledsck_low);
	AW2013_delay_1us(2);
	pinctrl_select_state(breahtlight_pinctrl, breahledsda_low);
	pinctrl_select_state(breahtlight_pinctrl, breahledsck_high);
	AW2013_delay_1us(2);
	pinctrl_select_state(breahtlight_pinctrl, breahledsda_high);
}

static char AW2013_i2c_write_byte(unsigned char data)
{
	int i;
	char ack;
//	mt_set_gpio_dir_base(AW2013_SDA_PIN,1);
	for(i=0; i<8; i++)
	{

		if (data & 0x80) {
			//AW2013_SDA_PIN_HIGH;
			pinctrl_select_state(breahtlight_pinctrl, breahledsda_high);
		}
		else {
			//AW2013_SDA_PIN_LOW;
			pinctrl_select_state(breahtlight_pinctrl, breahledsda_low);
		}
		data <<= 1;
		
		AW2013_delay_1us(1);
		//AW2013_SCK_PIN_HIGH;
		pinctrl_select_state(breahtlight_pinctrl, breahledsck_high);
		AW2013_delay_1us(1);
		//AW2013_SCK_PIN_LOW;
		pinctrl_select_state(breahtlight_pinctrl, breahledsck_low);
		AW2013_delay_1us(1);
		
		
	}
/*
	mt_set_gpio_dir_base(AW2013_SDA_PIN,0);
	AW2013_delay_1us(6);
	AW2013_SCK_PIN_HIGH;
	ack = AW2013_SDA_PIN_READ; /// ack   
	AW2013_delay_1us(1);
	AW2013_SCK_PIN_LOW;
*/

	pinctrl_select_state(breahtlight_pinctrl, breahledsda_in);
	AW2013_delay_1us(6);
	pinctrl_select_state(breahtlight_pinctrl, breahledsck_high);
	ack = gpio_get_value(AW2013_SDA_PIN); //AW2013_SDA_PIN_READ; /// ack  
	AW2013_delay_1us(1);
	pinctrl_select_state(breahtlight_pinctrl, breahledsck_low);

	return ack;	
}

static unsigned char AW2013_i2c_read_byte(void)
{
	int i;
	unsigned char bData;
//	mt_set_gpio_dir_base(AW2013_SDA_PIN,0);
	pinctrl_select_state(breahtlight_pinctrl, breahledsda_in);
	  bData = 0x00;
	  for (i=0;i<8;i++) {
		  bData <<= 1;
		  AW2013_delay_1us(4);
		  //AW2013_SCK_PIN_HIGH;
		  pinctrl_select_state(breahtlight_pinctrl, breahledsck_high);
		  if (gpio_get_value(AW2013_SDA_PIN)) //AW2013_SDA_PIN_READ
		  {
			  bData |= 0x01;
		  } else 
		  {
			  bData &= 0xfe;
		  }
		  AW2013_delay_1us(1);
		  //AW2013_SCK_PIN_LOW;
		  pinctrl_select_state(breahtlight_pinctrl, breahledsck_low);
	  }
/*
	  AW2013_delay_1us(1);
	  AW2013_SCK_PIN_HIGH;	
	  AW2013_delay_1us(1);
	  AW2013_SCK_PIN_LOW;
*/

	AW2013_delay_1us(1);
	pinctrl_select_state(breahtlight_pinctrl, breahledsck_high);
	AW2013_delay_1us(1);	
	pinctrl_select_state(breahtlight_pinctrl, breahledsck_low);

	  return bData;
}

static char AW2013_i2c_write_reg_org(unsigned char reg,unsigned char data)
{
	char ack=0;
	AW2013_i2c_start();	
	ack=AW2013_i2c_write_byte(IIC_ADDRESS_WRITE); 	//write device address
	if (ack)
	{
		//AW2013_i2c_stop();
		//return 1;
	}
	ack=AW2013_i2c_write_byte(reg);  	// reg address
	if (ack)
	{
		//AW2013_i2c_stop();
		//return 1;
	}
	ack = AW2013_i2c_write_byte(data);	// data
	if (ack)
	{
		//AW2013_i2c_stop();
		//return 1;
	}
	AW2013_i2c_stop();
	return ack;
}
static char AW2013_i2c_write_reg(unsigned char reg,unsigned char data)
{
	char ack=0;
	unsigned char i;
	for (i=0;i<AW2013_I2C_MAX_LOOP;i++)
	{
		ack=AW2013_i2c_write_reg_org(reg,data);
		if(ack==0) // ack success
			break;
	}
	return ack;
}

unsigned char AW2013_i2c_read_reg(unsigned char regaddr) 
{
	unsigned char i, bData;
	char ack1,ack2,ack3;
//	unsigned char i2caddr;
	for (i=0;i<AW2013_I2C_MAX_LOOP;i++)
	{
		AW2013_i2c_start();	
		ack1=AW2013_i2c_write_byte(IIC_ADDRESS_WRITE); 	//write device address
		ack2=AW2013_i2c_write_byte(regaddr);  	// reg address
		AW2013_i2c_stop();
		AW2013_i2c_start();	
		ack3=AW2013_i2c_write_byte(IIC_ADDRESS_READ); 	//write device address
		if((ack1 && ack2 && ack3)==0) // ack success
			break;
	}
	bData=AW2013_i2c_read_byte();
	AW2013_i2c_stop();
	return bData;
}


static void AW2013_breath_all(void)  //led on=0x01   ledoff=0x00
{  

	//write_reg(0x00, 0x55);				// Reset 
	AW2013_i2c_write_reg(0x01, 0x01);		// enable LED 不使用中断		

	AW2013_i2c_write_reg(0x31, Imax);	//config mode, IMAX = 5mA	
	AW2013_i2c_write_reg(0x32, Imax);	//config mode, IMAX = 5mA	
	AW2013_i2c_write_reg(0x33, Imax);	//config mode, IMAX = 5mA	

	AW2013_i2c_write_reg(0x34, 0xff);	// LED0 level,
	AW2013_i2c_write_reg(0x35, 0xff);	// LED1 level,
	AW2013_i2c_write_reg(0x36, 0xff);	// LED2 level,
											
	AW2013_i2c_write_reg(0x37, Rise_time<<4 | Hold_time);	//led0  上升时间，保持时间设定							
	AW2013_i2c_write_reg(0x38, Fall_time<<4 | Off_time);	       //led0 下降时间，关闭时间设定
	AW2013_i2c_write_reg(0x39, Delay_time<<4| Period_Num);   //led0  呼吸延迟时间，呼吸周期设定

	AW2013_i2c_write_reg(0x3a, Rise_time<<4 | Hold_time);	//led1上升时间，保持时间设定								
	AW2013_i2c_write_reg(0x3b, Fall_time<<4 | Off_time);	       //led1 下降时间，关闭时间设定
	AW2013_i2c_write_reg(0x3c, Delay_time<<4| Period_Num);   //led1  呼吸延迟时间，呼吸周期设定

	AW2013_i2c_write_reg(0x3d, Rise_time<<4 | Hold_time);	//led2  上升时间，保持时间设定				
	AW2013_i2c_write_reg(0x3e, Fall_time<<4 | Off_time);	       //led2 下降时间，关闭时间设定
	AW2013_i2c_write_reg(0x3f, Delay_time<<4| Period_Num);    //呼吸延迟时间，呼吸周期设定

	AW2013_i2c_write_reg(0x30, 0x07);	       //led on=0x01 ledoff=0x00	
	AW2013_delay_1us(8);//需延时5us以上	
 
}
static void AW2013_R_LED_on(void)  //led on=0x01   ledoff=0x00
{
	AW2013_i2c_write_reg(0x01, 0x01);		// enable LED 不使用中断
	AW2013_i2c_write_reg(0x33, Imax);	//config mode, IMAX = 5mA
	AW2013_i2c_write_reg(0x36, 0xff);	// LED2 level,	
	AW2013_i2c_write_reg(0x30, 0x04);	       //led on=0x01 ledoff=0x00
	
}
static void AW2013_G_LED_on(void)  //led on=0x01   ledoff=0x00
{
	AW2013_i2c_write_reg(0x01, 0x01);		// enable LED 不使用中断
	AW2013_i2c_write_reg(0x32, Imax);	//config mode, IMAX = 5mA
	AW2013_i2c_write_reg(0x35, 0xff);	// LED2 level,	
	AW2013_i2c_write_reg(0x30, 0x02);	       //led on=0x01 ledoff=0x00
	
}
static void AW2013_B_LED_on(void)  //led on=0x01   ledoff=0x00
{
	AW2013_i2c_write_reg(0x01, 0x01);		// enable LED 不使用中断
	AW2013_i2c_write_reg(0x31, Imax);	//config mode, IMAX = 5mA
	AW2013_i2c_write_reg(0x34, 0xff);	// LED2 level,	
	AW2013_i2c_write_reg(0x30, 0x01);	       //led on=0x01 ledoff=0x00
	
}
static void AW2013_breath_R_LED(void)  //led on=0x01   ledoff=0x00
{
	AW2013_i2c_write_reg(0x01, 0x01);		// enable LED 不使用中断
	AW2013_i2c_write_reg(0x33, Imax|0x70);	//config mode, IMAX = 5mA
	AW2013_i2c_write_reg(0x36, 0xff);	// LED2 level,	
	AW2013_i2c_write_reg(0x3d, Rise_time<<4 | Hold_time);	//led2  上升时间，保持时间设定				
	AW2013_i2c_write_reg(0x3e, Fall_time<<4 | Off_time);	       //led2 下降时间，关闭时间设定
	AW2013_i2c_write_reg(0x3f, Delay_time<<4| Period_Num);    //呼吸延迟时间，呼吸周期设定
	AW2013_i2c_write_reg(0x30, 0x04);	       //led on=0x01 ledoff=0x00
	AW2013_delay_1us(16);//需延时5us以上	
	
}
static void AW2013_breath_G_LED(void)  //led on=0x01   ledoff=0x00
{
	AW2013_i2c_write_reg(0x01, 0x01);		// enable LED 不使用中断
	AW2013_i2c_write_reg(0x32, Imax|0x70);	//config mode, IMAX = 5mA
	AW2013_i2c_write_reg(0x35, 0xff);	// LED2 level,
	AW2013_i2c_write_reg(0x3a, Rise_time<<4 | Hold_time);	//led1上升时间，保持时间设定								
	AW2013_i2c_write_reg(0x3b, Fall_time<<4 | Off_time);	       //led1 下降时间，关闭时间设定
	AW2013_i2c_write_reg(0x3c, Delay_time<<4| Period_Num);   //led1  呼吸延迟时间，呼吸周期设定
	AW2013_i2c_write_reg(0x30, 0x02);	       //led on=0x01 ledoff=0x00
	AW2013_delay_1us(16);//需延时5us以上	
	
}
static void AW2013_breath_B_LED(void)  //led on=0x01   ledoff=0x00
{
	AW2013_i2c_write_reg(0x01, 0x01);		// enable LED 不使用中断
	AW2013_i2c_write_reg(0x31, Imax|0x70);	//config mode, IMAX = 5mA
	AW2013_i2c_write_reg(0x34, 0xff);	// LED2 level,	
	AW2013_i2c_write_reg(0x37, Rise_time<<4 | Hold_time);	//led0  上升时间，保持时间设定							
	AW2013_i2c_write_reg(0x38, Fall_time<<4 | Off_time);	       //led0 下降时间，关闭时间设定
	AW2013_i2c_write_reg(0x39, Delay_time<<4| Period_Num);   //led0  呼吸延迟时间，呼吸周期设定
	AW2013_i2c_write_reg(0x30, 0x01);	       //led on=0x01 ledoff=0x00
	AW2013_delay_1us(16);//需延时5us以上	
	
}
void led_off_AW2013(void)
{
//	unsigned char reg_data;
//	unsigned int	reg_buffer[8];

	AW2013_i2c_write_reg(0x30, 0);				//led off	
	AW2013_i2c_write_reg(0x01,0);
}
void AW2013_init(void)
{
	AW2013_breath_all();
	led_off_AW2013();
}

static u8 bird_breathled_flag=0x00; 
   //0x00--6517
   //0x01--6577

ssize_t bird_breathled_read(struct file * file,char * buf,size_t count,loff_t * f_ops)
{
	char sdas[4]={0,0,0,0};
	ssize_t            status = 1;
	unsigned long    missing;

	sdas[0]=bird_breathled_flag;
	missing = copy_to_user(buf, sdas, status);
	if (missing == status)
	status = -EFAULT;
	else
	status = status - missing;
	
	BREATHLED_LOG("bird_breathled_read= %d\n",bird_breathled_flag);

	return status ; 
}



ssize_t bird_breathled_write (struct file * file,const char * buf, size_t count,loff_t * f_ops)
{
	unsigned long missing;
	char inbuffer[4]={0};
	int i;
	if(count>4)count=4;
	missing = copy_from_user(inbuffer, buf, count);
		
	for(i=0;i<count;i++)
	{
	    BREATHLED_LOG("bird_breathled_write[%d]=%d\n",i,inbuffer[i]);
	} 
	/*char'1'= 0x31  ,49*/
	if(inbuffer[0]==0x31)
	AW2013_breath_all();
	if(inbuffer[0]==0x32)
	led_off_AW2013();
	if(inbuffer[0]==0x33)
	AW2013_R_LED_on();
	if(inbuffer[0]==0x34)
	AW2013_G_LED_on();
	if(inbuffer[0]==0x35)
	AW2013_B_LED_on();
	if(inbuffer[0]==0x36)
	AW2013_breath_R_LED();
	if(inbuffer[0]==0x37)
	AW2013_breath_G_LED();
	if(inbuffer[0]==0x38)
	AW2013_breath_B_LED();	  

	BREATHLED_LOG("bird_breathled_flag=%d\n",bird_breathled_flag);

	return count;
}

#ifdef CONFIG_COMPAT
s32 bird_breathled_open(struct inode * inode,struct file * file) {
#else
ssize_t bird_breathled_open(struct inode * inode,struct file * file) {
#endif

	BREATHLED_LOG("bird_breathled_open()\n");   
	return 0;
}

#ifdef CONFIG_COMPAT
s32 bird_breathled_release(struct inode * inode, struct file * file) {
#else
ssize_t bird_breathled_release(struct inode * inode, struct file * file) {
#endif
	BREATHLED_LOG("bird_breathled_release()\n");   
	return 0;
}


/*****************************************************************************/
/* Kernel interface */
static const struct file_operations bird_breathled_ctl_ops={
    .owner        = THIS_MODULE,
    .open    = bird_breathled_open,
    .read    =bird_breathled_read,
    .write    =bird_breathled_write,
   // .unlocked_ioctl     = bird_breathled_ioctl,
    .release     =bird_breathled_release,
};


static int bird_breathled_probe(struct platform_device *dev)
{
    int ret = 0, err = 0;
       if(global_flag==1)
           return 0;
	BREATHLED_LOG("[bird_breathled_probe] start\n");
    ret = alloc_chrdev_region(&bird_breathled_devno, 0, 1, BIRD_BREATHLED_DEVNAME);
    if (ret) {
        BREATHLED_LOG("[bird_breathled_probe] alloc_chrdev_region fail: %d\n", ret);
        return -1;
        //goto bird_breathled_probe_error;
    } else {
        BREATHLED_LOG("[bird_breathled_probe] major: %d, minor: %d\n", MAJOR(bird_breathled_devno), MINOR(bird_breathled_devno));
    }
    cdev_init(&bird_breathled_cdev, &bird_breathled_ctl_ops);
    bird_breathled_cdev.owner = THIS_MODULE;
    err = cdev_add(&bird_breathled_cdev, bird_breathled_devno, 1);
    if (err) {
        BREATHLED_LOG("[bird_breathled_probe] cdev_add fail: %d\n", err);
        unregister_chrdev_region(bird_breathled_devno, 1);
        return -1;
      //  goto bird_breathled_probe_error;
    }

    bird_breathled_class = class_create(THIS_MODULE, "bird_breathleddrv");
    if (IS_ERR(bird_breathled_class)) {
        BREATHLED_LOG("[bird_breathled_probe] Unable to create class, err = %d\n", (int)PTR_ERR(bird_breathled_class)); 
        cdev_del(&bird_breathled_cdev); 
        unregister_chrdev_region(bird_breathled_devno, 1);
        return -1;
       // goto bird_breathled_probe_error;
    }

    bird_breathled_device = device_create(bird_breathled_class, NULL, bird_breathled_devno, NULL, BIRD_BREATHLED_DEVNAME);
    if(NULL == bird_breathled_device){
        BREATHLED_LOG("[bird_breathled_probe] device_create fail\n");
        class_destroy(bird_breathled_class);
        cdev_del(&bird_breathled_cdev);
        unregister_chrdev_region(bird_breathled_devno, 1);  
        return -1;
        //goto bird_breathled_probe_error;
    }

	breahtlight_gpio_init(dev);

	//bird_get_chip_id();
	//AW2013_breath_all();  //20150921 zxw modify for driver test 
	//AW2013_init();
 
    BREATHLED_LOG("bird_breathled_probe Done\n");
    global_flag=1;
    return 0;
/*
bird_breathled_probe_error:

    if (err == 0)
        cdev_del(&bird_breathled_cdev);
    if (ret == 0)
        unregister_chrdev_region(bird_breathled_devno, 1);

    return -1;
*/
}

static int bird_breathled_remove(struct platform_device *dev)
{
#ifdef ALLOC_DEVNO
    cdev_del(&bird_hall_cdev);
    unregister_chrdev_region(bird_breathled_devno, 1);
#else
    unregister_chrdev(MAJOR(bird_breathled_devno), BIRD_BREATHLED_DEVNAME);
#endif
    device_destroy(bird_breathled_class, bird_breathled_devno);
    class_destroy(bird_breathled_class);
    global_flag=0;
    BREATHLED_LOG("bird_hall_remove Done\n");
    return 0;
}

#ifdef CONFIG_OF
static const struct of_device_id breathled_of_match[] = {
	{.compatible = "mediatek,breathlight"},
	{},
};
#endif

static struct platform_driver breathled_driver = {
	.probe = bird_breathled_probe,
	.remove	= bird_breathled_remove,
	.driver = {
		.name	= "breathled",
		.owner	= THIS_MODULE,
#ifdef CONFIG_OF
	.of_match_table = breathled_of_match,
#endif
	},
};

#ifdef CONFIG_OF
struct platform_device breathled_device = {
	.name = "breathled",
	.id = -1,
};
#endif

static int __init bird_breathled_init(void)
{
	int ret;

	BREATHLED_LOG("%s\n", __func__);

#ifdef CONFIG_OF
	ret = platform_device_register(&breathled_device);
	if (ret)
		BREATHLED_LOG("bird_breathled_device_init:dev:E%d\n", ret);
#endif

	ret = platform_driver_register(&breathled_driver);


	if (ret)
	{
		BREATHLED_LOG("bird_breathled_init:drv:E%d\n", ret);
		return ret;
	}

	return ret;
}

static void __exit bird_breathled_exit(void)
{
        #ifdef CONFIG_OF
	platform_device_unregister(&breathled_device);
        #endif
	platform_driver_unregister(&breathled_driver);
}

module_init(bird_breathled_init);
module_exit(bird_breathled_exit);

MODULE_AUTHOR("Bird Inc.");
MODULE_DESCRIPTION("BREATHLED for MediaTek MT65xx chip");
MODULE_LICENSE("GPL");
MODULE_ALIAS("BREATHLED");
