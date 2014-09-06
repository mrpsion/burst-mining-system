<h1>Burst Mining System</h1>


Burst Mining System is a BurstCoin pool miner and plot generator with a web user interface.

It also allows you to view your mining system all in one place. For instance if you multiple miners running
on multiple servers you can view their stats in one view.

<h2>Build</h2>

<h3>Requirements</h3>

Java JDK 1.7 or above<br/>
Maven<br/>

<h3>Command</h3>

mvn package

this will produce a jar called mining-system-1.0.jar in the 'target' folder.

<h2>Run</h2> 

Using a jvm 1.7 or above run;<br/>

java -jar mining-system-1.0.jar --pool.url=http://{pool.hostname}:{pool.port} 

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

These options show the default values, you can override any of these with the startup argument. For instance if you only
want to use 2 threads to generate plots you would start the mining system like this;

java -jar mining-system-1.0.jar --pool.url=http://{pool.hostname}:{pool.port} --plot.generation.threads=2


<h2>System Monitoring<h2>

This mining system allow you to nominate 1 miner (or more if you want, but it would just be more configuration to do) to 
give you visibility on all the other miners in your system.  To enable this simply include the following argument when you
start the nominated miner.

--system.hosts=miner1.host:port,miner2.host:port,miner3.host:port

you do not need to include the local miner in this list, it will fetch its own stats automatically.







