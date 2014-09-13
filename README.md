<h1>Burst Mining System</h1>


Burst Mining System is a BurstCoin pool miner and plot generator with a web user interface.

It also allows you to view your mining system all in one place. For instance if you multiple miners running
on multiple servers you can view their stats in one view.

This is a V2 Pool only mining client.  It supports both Uray and Official style v2 pools. If you are going to use the official pool, you will need set the pool.type

<h2>Build</h2>

<h3>Requirements</h3>

Java JDK 1.7 or above<br/>
Maven<br/>

<h3>Command</h3>

mvn package

this will produce a jar called mining-system-1.20.jar in the 'target' folder.

<h2>Run</h2> 

Using a jvm 1.7 or above run;<br/>

java -jar mining-system-1.20.jar --pool.url=http://{pool.hostname}:{pool.port} 

<h3>Other options</h3>

<b>--server.port=8180</b><br/>
The port number to bind to. (If you are running multiple miners on a single host, you will want to give each one a unique port number<br/>
<b>--netstat.update.time=10000</b><br/>
  How often to check with the pool on the current block<br/>
<b>--plotmonitor.update.time=5000</b><br/>
  How often in milliseconds to update the systems plot data. (just filesizes etc, useful when generating plots)<br/>
<b>--plot.folder=plots</b><br/>
  folder containing your plots.<br/>
<b>--plot.generation.threads=8</b><br/>
  How many threads to use for plot generation.<br/>
<b>--miner.threads=1</b><br/>
  Number of miner threads to use. The system is coded to allow multiple mining threads, but I would think it is probably unwise to use more than 1,as it may incur more stress on your harddrives.<br/>
<b>--system.update.time=30000</b><br/>
  How often, in milliseconds, to check with the other miners in the system to get their stats.<br/>
<b>--pool.type=uray / official</b><br/>
  The miner will assume uray style pool if this argument is not present. If you would like to mine on the official v2 pool you will need to add --pool.type=official<br/>

These options show the default values, you can override any of these with the startup argument. For instance if you only
want to use 2 threads to generate plots you would start the mining system like this;

java -jar mining-system-1.20.jar --pool.url=http://{pool.hostname}:{pool.port} --plot.generation.threads=2


<h2>System Monitoring</h2>

This mining system allow you to nominate 1 miner (or more if you want, but it would just be more configuration to do) to 
give you visibility on all the other miners in your system.  To enable this simply include the following argument when you
start the nominated miner.

--system.hosts=miner1.host:port,miner2.host:port,miner3.host:port

you do not need to include the local miner in this list, it will fetch its own stats automatically.


<h2>Usage</h2>

Once you have built the system and started it. You can use it.<br/>
It will automatically start mining any plots start startup or when a block changes.<br/>

To access the UI go to http://host:port where you installed the miner, the default port is 8180

To generate a plot, click the + icon in the plots section and fill in the information. Its the same data as the PocMiner, just without the threads, as this is a system level setting here.<br/>

Once you submit that form, you should see your new plot. Click the generate button next to the plot to begin the generation process.<br/>

You can generate multiple plots at the same time, but remember it will use the same number of threads for each plot, so you can quickly overload your system.<br/>

If this is a "master" node, i.e. one started with --system.hosts, you can view all other miners plots in the System Dashboard section.


Please report any issues you find<br/>


Burst: BURST-JSES-5WN7-PHD8-7CQJ6  Bitcoin: 1JiwTkmeztRh7WSD9DaRxkJCYxDfMJvKoJ




