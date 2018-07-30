package org.sodeac.eventdispatcher.itest.components.service;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.common.edservice.api.IEventDrivenService;
import org.sodeac.eventdispatcher.common.edservice.api.wiring.Capability;
import org.sodeac.eventdispatcher.common.edservice.api.wiring.capability.DomainCapability;
import org.sodeac.eventdispatcher.common.edservice.api.wiring.capability.ServiceCapability;

@Component
(
	service={IEventController.class,IEventDrivenService.class},
	property=
	{
		IEventDrivenService.PROPERTY_SERVICE_QUEUE_ID+"="+Service1.DOMAIN + ".queue"
	}
)
public class Service1 implements IEventController, IEventDrivenService
{
	public static final String DOMAIN = "org.sodeac.eventdispatcher.itest";
	public static final String SERVICE = "org.sodeac.eventdispatcher.itest.service1";

	@Override
	public List<Capability> getCapabilityList()
	{
		return Capability.toList(new Capability[]
		{
			new DomainCapability(Service1.DOMAIN),
			new ServiceCapability(Service1.SERVICE)
		});
	}
}
