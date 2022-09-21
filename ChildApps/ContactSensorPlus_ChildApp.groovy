/**
 *
 * Sensor Groups+_Contact
 *
 * Copyright 2022 Ryan Elliott
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * v1.0		RLE		Creation
 * v1.1     RLE     Added list attribute to show triggered devices
 * v1.2     RLE     Added threshold input and associated logic
 */
 
definition(
    name: "Sensor Groups+_Contact",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates a virtual device to track a group of contact sensors.",
    category: "Convenience",
	parent: "rle.sg+:Sensor Groups+",
	iconUrl: "",
    iconX2Url: "")

preferences {
    page(name: "prefContactGroup")
	page(name: "prefSettings")
}

def prefContactGroup() {
	return dynamicPage(name: "prefContactGroup", title: "Create a Contact Group", nextPage: "prefSettings", uninstall:true, install: false) {
		section {
            label title: "Enter a name for this child app. This will create a virtual contact sensor which reports the open/closed status based on the sensors you select.", required:true
		}
	}
}

def prefSettings() {
    return dynamicPage(name: "prefSettings", title: "", install: true, uninstall: true) {
		section {
			paragraph "Please choose which sensors to include in this group. When all the sensors are closed, the virtual device is closed. If any sensor is open, the virtual device is open."

			input "contactSensors", "capability.contactSensor", title: "Contact sensors to monitor", multiple:true, required:true
        }
		section {
            paragraph "Set how many sensors are required to change the status of the virtual device."
            
            input "activeThreshold", "number", title: "How many sensors must be open before the group is open? Leave set to one if any contact sensor open should make the group open.", required:false, defaultValue: 1
            
            input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false
        }
	}
}

def installed() {
	initialize()
}

def uninstalled() {
	logDebug "uninstalling app"
	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}
}

def updated() {	
    	logDebug "Updated with settings: ${settings}"
	unschedule()
    	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(contactSensors, "contact", contactHandler)
	createOrUpdateChildDevice()
    contactHandler()
    def device = getChildDevice(state.contactDevice)
    device.sendEvent(name: "TotalCount", value: contactSensors.size())
	device.sendEvent(name: "OpenThreshold", value: activeThreshold) 
    runIn(1800,logsOff)
}

def contactHandler(evt) {
    log.info "Contact changed, checking status count..."
    getCurrentCount()
    def device = getChildDevice(state.contactDevice)
	if (state.totalOpen >= activeThreshold)
	{
		log.info "Open threshold met; setting virtual device as open"
		logDebug "Current threshold value is ${activeThreshold}"
		device.sendEvent(name: "contact", value: "open", descriptionText: "The open devices are ${state.openList}")
	} else {
		log.info "All closed; setting virtual device as closed"
		logDebug "Current threshold value is ${activeThreshold}"
		device.sendEvent(name: "contact", value: "closed")
	}
}

def createOrUpdateChildDevice() {
	def childDevice = getChildDevice("contactgroup:" + app.getId())
    if (!childDevice || state.contactDevice == null) {
        logDebug "Creating child device"
		state.contactDevice = "contactgroup:" + app.getId()
		addChildDevice("rle.sg+", "Sensor Groups+_Virtual Contact Sensor", "contactgroup:" + app.getId(), 1234, [name: app.label + "_device", isComponent: false])
    }
	else if (childDevice && childDevice.name != (app.label + "_device"))
		childDevice.name = app.label + "_device"
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}

def logsOff(){
    log.warn "debug logging disabled..."
    app.updateSetting("debugOutput",[value:"false",type:"bool"])
}

def getCurrentCount() {
	def device = getChildDevice(state.contactDevice)
	def totalOpen = 0
    def totalClosed = 0
	def openList = []
	contactSensors.each { it ->
		if (it.currentValue("contact") == "open")
		{
			totalOpen++
			if (it.label) {
            openList.add(it.label)
            }
            else if (!it.label) {
                openList.add(it.name)
            }
		}
		else if (it.currentValue("contact") == "closed")
		{
			totalClosed++
		}
    }
    state.totalOpen = totalOpen
	if (openList.size() == 0) {
        openList.add("None")
    }
	state.openList = openList.sort()
    logDebug "There are ${totalClosed} sensors closed"
    logDebug "There are ${totalOpen} sensors open"
    device.sendEvent(name: "TotalClosed", value: totalClosed)
    device.sendEvent(name: "TotalOpen", value: totalOpen)
	device.sendEvent(name: "OpenList", value: state.openList)
}