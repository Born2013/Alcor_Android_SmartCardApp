#include <linux/of.h>
#include <linux/of_address.h>
#include <linux/of_irq.h>
#include <linux/gpio.h>

#include <linux/interrupt.h>
#include <linux/i2c.h>
#include <linux/slab.h>
#include <linux/irq.h>
#include <linux/miscdevice.h>
#include <asm/uaccess.h>
#include <linux/delay.h>
#include <linux/input.h>
#include <linux/workqueue.h>
#include <linux/kobject.h>
#include <linux/platform_device.h>
#include <asm/atomic.h>
#include <linux/version.h>
#include <linux/fs.h>   
#include <linux/wakelock.h> 
#include <asm/io.h>
#include <linux/module.h>
#include "../temperature.h"

#define AS6200_DEV_NAME		"as6200"

struct temperature_obj {
  int threshold_high;
  int threshold_low;
  int i2c_addr;
};


struct temperature_obj *as6200_obj;
struct i2c_client *g_client;
static int g_temp = 0;


static int  as6200_local_init(void);
static int  as6200_local_uninit(void);
static int as6200_i2c_probe(struct i2c_client *client, const struct i2c_device_id *id); 
static int as6200_i2c_remove(struct i2c_client *client);

static const struct i2c_device_id as6200_i2c_id[] = {{AS6200_DEV_NAME,0},{}};

static struct i2c_board_info __initdata as6200_i2c_temp[] = {
	{
		I2C_BOARD_INFO(AS6200_DEV_NAME, 0x49)	/* 0x2c */
	}
};


static struct tp_init_info as6200_init_info = {
		.name = "as6200",
		.init = as6200_local_init,
		.uninit = as6200_local_uninit,
	
};

static int as6200_read_block(struct i2c_client *client, u8 addr, u8 *data, u8 len)
{
	u8 beg = addr;
        int err; 
	struct i2c_msg msgs[2] = 
	{
		{
			.addr = client->addr,	 
			.flags = 0,
			.len = 1,				 
			.buf= &beg
		},
		{
			.addr = client->addr,	 
			.flags = I2C_M_RD,
			.len = len, 			 
			.buf = data,
		}
	};
	
	if (!client)
		return -EINVAL;
	else if (len > 8) 
	{		 
		printk(" length %d exceeds 8\n", len);
		return -EINVAL;
	}

	err = i2c_transfer(client->adapter, msgs, sizeof(msgs)/sizeof(msgs[0]));
	if (err != 2) 
	{
		printk("i2c_transfer error: (%d %p %d) %d\n", addr, data, len, err);
		err = -EIO;
	}
	else 
	{
		err = 0;
	}
	return err;
}

static int as6200_reg_init(void)
{
  int ret = 0;
  int dat_low = as6200_obj->threshold_low << 4;
  int dat_high = as6200_obj->threshold_high << 4;
  char databuf[3] = {0x01,0x45,0x80};
  char databufl[3] = {0x02,((dat_low >> 8) & 0xff),dat_low & 0xff};
  char databufh[3] = {0x03,((dat_high >> 8) & 0xff),dat_high & 0xff};
  ret = i2c_master_send(g_client, databuf, 0x03);
  if(ret <= 0)
  {
     printk("%s, i2c err, addr = %x, flag = %d\n", __func__, g_client->addr, g_client->flags);
     return -1;
  }

  ret = i2c_master_send(g_client, databufl, 0x03);
  if(ret <= 0)
  {
     printk("%s, i2c err, addr = %x, flag = %d\n", __func__, g_client->addr, g_client->flags);
     return -1;
  }

  ret = i2c_master_send(g_client, databufh, 0x03);
  if(ret <= 0)
  {
     printk("%s, i2c err, addr = %x, flag = %d\n", __func__, g_client->addr, g_client->flags);
     return -1;
  }
  
  return 0;
}


static u16 as6200_util(u16 *dat)
{
   u16 i,j;
   u16 tmp = *dat;
   u16 new_tmp = 0;
   tmp = (tmp >> 4) - 1;
   for(i = 0;i < 12;i++)
   {
        j = tmp & (1 << i);
        if(j != 0)
        new_tmp = new_tmp | (0 << i);
        else
        new_tmp = new_tmp | (1 << i);
   } 
   return  new_tmp;  
}

static int as6200_open(struct inode *inode, struct file *file)
{
        printk("as6200_open\n");
        return 0;
}

static int as6200_release(struct inode *inode, struct file *file)
{
        printk("as6200_release\n");
        return 0;
}

static long as6200_ioctl(struct file *file, unsigned int cmd, unsigned long arg)
{       
        return 0;
}

static struct file_operations as6200_fops = {
        .owner = THIS_MODULE,
        .open = as6200_open,
        .release = as6200_release,
        .unlocked_ioctl = as6200_ioctl,
};

static struct miscdevice as6200_device = {
        .minor = MISC_DYNAMIC_MINOR,
        .name = AS6200_DEV_NAME,
        .fops = &as6200_fops,
};

static int as6200_open_report_data(int open)
{
       
        printk("as6200: %s\n",__func__);
	return 0;
}

static int as6200_enable_nodata(int en)
{
        int ret = 0;
	char databuf[3] = {0x01,0x44,0x80};
        char databuf1[3] = {0x01,0x45,0x80};
        printk("as6200: %s\n",__func__);
        if(en)
        {
        printk("as6200_enable\n");
        ret = i2c_master_send(g_client, databuf, 0x03);
        }
        else
        {
        printk("as6200_disable\n");
        ret = i2c_master_send(g_client, databuf1, 0x03);
        }
        if(ret <= 0)
        {
          printk("%s, i2c1 err, addr = %x, flag = %d\n", __func__, g_client->addr, g_client->flags);
          return -1;
        }
	return 0;
}

static int as6200_set_delay(u64 ns)
{
        printk("as6200: %s\n",__func__);
	return 0;
}

static int as6200_get_data(int* value, int* status)
{
	int err = 0;
        u8 buf[2];
        u16 temp = 0;
        printk("as6200: %s\n",__func__);
        as6200_read_block(g_client,0x00,buf,2); 
        if(g_temp == 0)
        g_temp = 20;
        else
        g_temp = 0;
        temp = ((buf[0] << 8) | buf[1]);
        if((buf[0]&0x80) == 0)
        {
          
          *value = (temp >> 4)*625 + g_temp;
        }
        else
        {       
          *value = 0 - (as6200_util(&temp)*625) + g_temp;        
        }	
        *status = 0;
	return err;
}

static int as6200_i2c_probe(struct i2c_client *client, const struct i2c_device_id *id)
{
  int ret = 0;
  int high[] = {0};
  int i2c_addr[4] = {0};
  int low[] = {0};
  u8 buf[2];
  const char *name = "mediatek,as6200";
  struct device_node *np;
  struct tp_control_path tp_ctl={0};
  struct tp_data_path tp_data={0};
  printk("as6200_i2c_probe in\n");
  if(!(as6200_obj = kzalloc(sizeof(*as6200_obj), GFP_KERNEL)))
  {
   ret = -ENOMEM;
   return ret;
  }
  np = of_find_compatible_node(NULL, NULL, name);
  if (np) 
  {
  ret = of_property_read_u32_array(np , "temperature_threshold_high", high, ARRAY_SIZE(high));
  if(ret == 0)
  as6200_obj->threshold_high = high[0];
  ret = of_property_read_u32_array(np , "temperature_threshold_low", low, ARRAY_SIZE(low));
  if(ret == 0)
  as6200_obj->threshold_low = low[0];
  ret = of_property_read_u32_array(np , "i2c_addr", i2c_addr, ARRAY_SIZE(i2c_addr));
  if (ret == 0) 
  as6200_obj->i2c_addr = i2c_addr[0];
  }
  else
  {
  printk("parse dts is error\n");
  return -1;
  }
  g_client = client;
  g_client->addr = as6200_obj->i2c_addr;
  misc_register(&as6200_device);
  as6200_reg_init();	
  as6200_read_block(g_client,0x02,buf,2);
  printk("buff[0]:%d,buf[1]:%d\n",buf[0],buf[1]);
  tp_ctl.open_report_data= as6200_open_report_data;
  tp_ctl.enable_nodata = as6200_enable_nodata;
  tp_ctl.set_delay  = as6200_set_delay;
  tp_ctl.is_report_input_direct = false;
  tp_ctl.is_support_batch = false;	
  ret = tp_register_control_path(&tp_ctl);
  if(ret)
  {
    g_client = NULL;
    misc_deregister(&as6200_device);
    printk("tp_register_control_path register fail = %d\n", ret);
    return ret;
  }
  tp_data.get_data = as6200_get_data;
  tp_data.vender_div = 1;
  ret = tp_register_data_path(&tp_data);	
  if(ret)
  {
    g_client = NULL;
    misc_deregister(&as6200_device);
    printk("tp_register_data_path register fail = %d\n", ret);
    return ret;
  }	
  printk("as6200_i2c_probe out\n");
  return ret;
  
}

static int as6200_i2c_remove(struct i2c_client *client)
{
       
        g_client = NULL;
        misc_deregister(&as6200_device);
        printk("ltr303_remove\n");
        return 0;
}

#ifdef CONFIG_OF
static const struct of_device_id temperature_of_match[] = {
	{.compatible = "mediatek,as6200"},
	{},
};
#endif

static struct i2c_driver as6200_i2c_driver = {
	 .driver = {
	             .name = AS6200_DEV_NAME,
	  #ifdef CONFIG_OF
		     .of_match_table = temperature_of_match,
	  #endif
	 },
	 .probe = as6200_i2c_probe,
	 .remove = as6200_i2c_remove,
	 .id_table = as6200_i2c_id,
};


static int as6200_local_init(void) 
{
        printk("as6200: %s\n",__func__);
        if(i2c_add_driver(&as6200_i2c_driver))
        {
          printk("as6200 add driver error\n");
          return -1;
        } 
        printk("as6200_init Out\n");
	return 0;
}

static int  as6200_local_uninit(void)
{

	printk("as6200: %s\n",__func__); 
	i2c_del_driver(&as6200_i2c_driver);
	return 0;
}

static int __init as6200_init(void)
{
  int ret = 0;
  struct i2c_client *client;
  struct i2c_adapter *i2c_adap = i2c_get_adapter(2);
  printk("as62000_init In\n");
  if(i2c_adap)
  {
     client = i2c_new_device(i2c_adap, &as6200_i2c_temp[0]);          
  }
  ret = tp_driver_add(&as6200_init_info);
  if(ret)
  {
     printk("as6200 tp_driver_add is error\n");
   }
  printk("as6200_init Out\n");
  return 0;
}

static void __exit as6200_exit(void)
{
  printk("as62000_exit In\n");    
  i2c_del_driver(&as6200_i2c_driver);
  printk("as62000_exit out\n");
}
module_init(as6200_init);
module_exit(as6200_exit);

MODULE_AUTHOR("bddriver dayin");
MODULE_DESCRIPTION("as6200 temperature driver");
MODULE_LICENSE("GPL");
