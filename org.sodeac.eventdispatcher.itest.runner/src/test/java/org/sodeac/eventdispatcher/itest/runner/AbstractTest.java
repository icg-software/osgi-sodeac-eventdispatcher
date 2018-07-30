/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.itest.runner;

import org.ops4j.pax.exam.Option;

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.options.ProvisionOption;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.sodeac.eventdispatcher.api.IEventDispatcher;

import java.io.File;

public abstract class AbstractTest
{
	protected boolean testStrictTime = true;
	
	public static ProvisionOption<?> reactorBundle(String artifactId, String version) 
	{
		String fileName = String.format("%s/../%s/target/%s-%s.jar", PathUtils.getBaseDir(), artifactId, artifactId,version);

		if (new File(fileName).exists()) 
		{
			try
			{
				String url = "file:" + new File(fileName).getCanonicalPath();
				return bundle(url);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			fileName = String.format("%s/../%s/target/%s-%s-SNAPSHOT.jar", PathUtils.getBaseDir(), artifactId, artifactId,version);

			if (new File(fileName).exists()) 
			{
				try
				{
					String url = "file:" + new File(fileName).getCanonicalPath();
					return bundle(url);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public static String getBundleStateName(int state)
	{
		switch (state) 
		{
			case Bundle.UNINSTALLED:
					
				return "UNINSTALLED";
				
			case Bundle.INSTALLED:
				
				return "INSTALLED";
	
			case Bundle.RESOLVED:
				
				return "RESOLVED";
			
			case Bundle.STARTING:
				
				return "STARTING";
			
			case Bundle.STOPPING:
				
				return "STOPPING";
				
			case Bundle.ACTIVE:
				
				return "ACTIVE";
			default:
				
				return "State " + state;
		}
	}

	public Option[] config() 
	{
		MavenArtifactUrlReference karafUrl = maven()
			.groupId("org.apache.karaf")
			.artifactId("apache-karaf")
			.version("4.1.5")
			.type("zip");

		MavenUrlReference karafStandardRepo = maven()
			.groupId("org.apache.karaf.features")
			.artifactId("standard")
			.version("4.1.5")
			.classifier("features")
			.type("xml");
		
		return new Option[] 
		{
			karafDistributionConfiguration()
				.frameworkUrl(karafUrl)
				.unpackDirectory(new File("target", "exam"))
				.useDeployFolder(false),
			editConfigurationFileExtend
			(
				"etc/org.ops4j.pax.url.mvn.cfg", 
				"org.ops4j.pax.url.mvn.repositories", 
				", https://oss.sonatype.org/content/repositories/snapshots@id=sonatype.snapshots.deploy@snapshots@noreleases"
			),
			//repositories(repository("https://oss.sonatype.org/content/repositories/snapshots@id=sonatype.snapshots.deploy").allowSnapshots().disableReleases()),
			keepRuntimeFolder(),
			cleanCaches( true ),
			logLevel(LogLevel.INFO),
			features(karafStandardRepo , "scr"),
			mavenBundle("io.dropwizard.metrics", "metrics-core", "3.2.6").start(),
			mavenBundle("org.easymock", "easymock", "3.4").start(),
			mavenBundle("org.sodeac","org.sodeac.xuri","0.9.0-SNAPSHOT").start(),
			mavenBundle("com.googlecode.json-simple", "json-simple", "1.1.1").start(),
			mavenBundle("com.google.guava", "guava", "25.1-jre").start(),
			reactorBundle("org.sodeac.eventdispatcher.api","0.9.3").start(),
			reactorBundle("org.sodeac.eventdispatcher.provider","0.9.5").start(),
			reactorBundle("org.sodeac.eventdispatcher.extension.jmx","0.9.5").start(),
			reactorBundle("org.sodeac.eventdispatcher.common","0.9.3").start(),
			reactorBundle("org.sodeac.eventdispatcher.itest.components","0.9.0").start()
		};
	}
	
	public void waitQueueIsUp(IEventDispatcher eventDispatcher, String queueId, long timeOut)
	{
		long timeOutTimestamp = System.currentTimeMillis() + timeOut;
		while(timeOutTimestamp > System.currentTimeMillis())
		{
			if(eventDispatcher.getQueue(queueId) != null)
			{
				break;
				
			}
			try {Thread.sleep(108);}catch (Exception e) {}
		}
	}
	
	public boolean checkTimeMeasure(long idealValue, long realValue, long absoluteTolerance, double relativeToleranceInPercent )
	{
		// sometimes travis-ci lacks => running stops for more than on second
		return checkTimeMeasure(idealValue, realValue, absoluteTolerance, relativeToleranceInPercent, 3000);
	}
	
	public boolean checkTimeMeasure(long idealValue, long realValue, long absoluteTolerance, double relativeToleranceInPercent, long notRealtimeSystemOffsetTolerance )
	{
		if(idealValue == realValue)
		{
			return true;
		}
		
		boolean neg = false;
		long diff = realValue - idealValue;
		if(diff < 0)
		{
			neg = true;
			diff = -1 * diff;
		}
		if(absoluteTolerance > 0)
		{
			if(diff <= absoluteTolerance)
			{
				return true;
			}
			if(! neg)
			{
				if(diff <= ( absoluteTolerance + notRealtimeSystemOffsetTolerance))
				{
					return true;
				}
			}
		}
		
		if((relativeToleranceInPercent >= 0.0) && (relativeToleranceInPercent <= 100.0))
		{
			double relDiff = ((idealValue / 100.0) * relativeToleranceInPercent);
			if(relDiff <= (double)diff)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean checkMetric(double idealValue, double realValue, double absoluteTolerance, double relativeToleranceInPercent )
	{
		if(idealValue == realValue)
		{
			return true;
		}
		
		double diff = realValue - idealValue;
		if(diff < 0)
		{
			diff = -1 * diff;
		}
		if(absoluteTolerance > 0)
		{
			if(diff <= absoluteTolerance)
			{
				return true;
			}
		}
		
		if((relativeToleranceInPercent >= 0.0) && (relativeToleranceInPercent <= 100.0))
		{
			double relDiff = ((idealValue / 100.0) * relativeToleranceInPercent);
			if(relDiff <= (double)diff)
			{
				return true;
			}
		}
		return false;
	}
	
}
