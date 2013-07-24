package net.charter.orion_pax.OasisLottery;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import net.milkbowl.vault.economy.Economy;

public class OasisLottery extends JavaPlugin implements Listener{
	public static Economy econ = null;
	public static double balance;
	public static boolean hasDoubled = false;
	public static long lastdate = 0;
	public static String lastwinner;
	public static double lastwinnings = 0;
	public static int lottocount;
	public int luckyday = randomNum(1,1000000);
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
	public static GregorianCalendar dateoflastwinner = new GregorianCalendar(TimeZone.getTimeZone("US/Eastern"));
	BukkitTask task;

	@Override
	public void onEnable(){
		this.saveDefaultConfig();
		if (!setupEconomy() ) {
			this.getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		balance = this.getConfig().getDouble("balance");
		hasDoubled = this.getConfig().getBoolean("hasdoubled");
		lastdate = this.getConfig().getLong("lastdate");
		lastwinner = this.getConfig().getString("lastwinner");
		lastwinnings = this.getConfig().getDouble("lastwinnings");

		lottocount = this.getConfig().getConfigurationSection("lottery").getKeys(false).size();
		if (lastdate!=0){
			dateoflastwinner.setTimeInMillis(lastdate);
		}

		task = this.getServer().getScheduler().runTaskTimer(this, new Runnable(){
			@Override
			public void run(){
				saveConfig();
				if(!hasDoubled){
					if(balance!=0){
						if (luckyday == randomNum(1,1000000)) {
							balance = balance * 2;
							hasDoubled = true;
							getConfig().set("balance", balance);
							getConfig().set("hasdoubled", hasDoubled);
							saveConfig();
							getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&aTHE JACKPOT HAS DOUBLED!"));
						}
					}
				}
			}
		}, 0, 20L);
	}

	@Override
	public void onDisable(){
		this.saveConfig();
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public int randomNum(Integer lownum, double d) {
		//Random rand = new Random();
		int randomNum = lownum + (int)(Math.random() * ((d - lownum) + 1));
		//int randomNum = rand.nextInt(highnum - lownum + 1) + lownum;
		return randomNum;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("lottoplay")){
			if(args.length==0){
				return false;
			}

			if (args.length==1){
				try { 
					Double.parseDouble(args[0]); 
				} catch(NumberFormatException e) { 
					sender.sendMessage(ChatColor.GOLD + args[0] + " is not an integer!");
					return true;
				}

				if (this.getConfig().contains("lottery." + sender.getName())){
					if (econ.hasAccount(sender.getName()) && econ.getBalance(sender.getName())>=Double.parseDouble(args[0])) {
						double pbalance = this.getConfig().getDouble("lottery." + sender.getName());
						pbalance = pbalance + Double.parseDouble(args[0]);
						this.getConfig().set("lottery." + sender.getName(), pbalance);
						econ.withdrawPlayer(sender.getName(), Double.parseDouble(args[0]));
						balance = balance + Double.parseDouble(args[0]);
						this.getConfig().set("balance", balance);
						this.saveConfig();
						return true;
					} else {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aDont have enough money!"));
						return true;
					}
				} else {
					lottocount++;
					this.getConfig().set("lottery." + sender.getName(), Double.parseDouble(args[0]));
					balance = balance + Double.parseDouble(args[0]);
					this.getConfig().set("balance", balance);
					this.saveConfig();
					return true;
				}
			}
		}

		if(cmd.getName().equalsIgnoreCase("lotto")){
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aOasis Lotter!"));
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a============"));
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aJackPot: " + balance));
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aTotal players: " + lottocount));
			if (this.getConfig().contains("lottery." + sender.getName())) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aMy spendings: " + this.getConfig().getDouble("lottery." + sender.getName())));
			}
			if (lastwinnings!=0) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aLast pick: " + sdf.format(dateoflastwinner.getTime())));
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aLast winner: " + lastwinner));
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aLast winnings: " + lastwinnings));
			}
			return true;
		}

		if(cmd.getName().equalsIgnoreCase("lottolist")){
			for(String pstring : this.getConfig().getConfigurationSection("lottery").getKeys(false)){
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + pstring + " : " + this.getConfig().getDouble("lottery." + pstring)));
			}
			return true;
		}

		if(cmd.getName().equalsIgnoreCase("lottoadd")){
			try { 
				balance = balance + Double.parseDouble(args[0]);
				this.getConfig().set("balance", balance);
				this.saveConfig();
				return true;
			} catch(NumberFormatException e) { 
				sender.sendMessage(ChatColor.GOLD + args[0] + " is not an integer!");
				return true;
			}
		}

		if(cmd.getName().equalsIgnoreCase("lottosub")){
			try { 
				balance = balance - Double.parseDouble(args[0]);
				this.getConfig().set("balance", balance);
				this.saveConfig();
				return true;
			} catch(NumberFormatException e) { 
				sender.sendMessage(ChatColor.GOLD + args[0] + " is not an integer!");
				return true;
			}
		}
		if (cmd.getName().equalsIgnoreCase("lottopick")){
			int mypick = randomNum(1,lottocount);
			int countme = 1;
			for(String mywinner : this.getConfig().getConfigurationSection("lottery").getKeys(false)){
				if(countme == mypick){
					this.getServer().broadcastMessage("And the winner is " + mywinner);
					econ.depositPlayer(mywinner, balance);
					lastwinner=mywinner;
					lastwinnings=balance;
					Date mydate = new Date();
					lastdate = mydate.getTime();
					
					reset();
					return true;
				}
				countme++;
			}
		}

		if (cmd.getName().equalsIgnoreCase("lottoreset")){
			reset();
			return true;
		}

		return false;
	}

	public void reset(){
		balance=0;
		hasDoubled=false;
		lottocount=0;
		luckyday = randomNum(1,1000000);
		File configFile = new File(this.getDataFolder(), "config.yml");
		if (configFile.exists()) {
			configFile.delete();
			this.saveDefaultConfig();
			this.getConfig().set("lottery", null);
			this.getConfig().set("lastwinner", lastwinner);
			this.getConfig().set("lastwinnings", lastwinnings);
			this.getConfig().set("lastdate", lastdate);
			this.getConfig().set("balance", balance);
			this.getConfig().set("hasdoubled", hasDoubled);
			this.saveConfig();
			this.reloadConfig();
		}
	}
	
	public void saveme(){
		this.getConfig().set("lastwinner", lastwinner);
		this.getConfig().set("lastwinnings", lastwinnings);
		this.getConfig().set("lastdate", lastdate);
		this.getConfig().set("balance", balance);
		this.getConfig().set("hasdoubled", hasDoubled);
	}
}
