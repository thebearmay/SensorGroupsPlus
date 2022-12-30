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
 * v1.3		RLE		Added switch attribute to follow contact attribute; updated UI; added delays for active/inactive
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
    page(name: "mainPage")
}

def mainPage() {
	return dynamicPage(name: "mainPage", uninstall:true, install: true) {
		section(getFormat("header","<b>App Name</b>")) {
            label title: "<b>Enter a name for this child app.</b>"+
            "<br>This will create a virtual contact sensor which reports the open/closed status based on the sensors you select.", required:true,width:6
		}

		section(getFormat("header","<b>Device Selection</b>")) {
			paragraph "<b>Please choose which sensors to include in this group.</b>"+
            "<br>The virtual device will report status based on the configured threshold."

			input "contactSensors", "capability.contactSensor", title: "Contact sensors to monitor", multiple:true, required:true,width:6
        }

		section(getFormat("header","<b>Options</b>")) {            
            input "activeThreshold", "number", title: "<b>How many sensors must be open before the group is open?</b><br>Leave set to one if any contact sensor open should make the group open.", required:false, defaultValue: 1,width:6
			paragraph ""
			input "delayActive", "number", title: "<b>Add a delay before activating the group device?</b><br>Optional, in seconds", required:false,width:6
			input "delayInactive", "number", title: "<b>Add a delay before deactivating the group device?</b><br>Optional, in seconds", required:false,width:6
			paragraph ""
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
    def device = getChildDevice(state.contactDevice)
	getCurrentCount()
	if (state.totalOpen >= activeThreshold) {devActive()} else {devInactive()}
    device.sendEvent(name: "TotalCount", value: contactSensors.size())
	device.sendEvent(name: "OpenThreshold", value: activeThreshold) 
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def contactHandler(evt) {
    logDebug "Contact changed, checking status count..."
    getCurrentCount()
	def newDelayActive = delayActive ?: 0
	def newDelayInactive = delayInactive ?: 0
	if (state.totalOpen >= activeThreshold)
	{
		if(newDelayActive >= 1) {
			logDebug "Open threshold met; delaying ${newDelayActive} seconds"
			unschedule(devInactive)
			runIn(newDelayActive,devActive)
		} else {
			unschedule(devInactive)
			devActive()
		}
	} else {
		if(newDelayInactive >= 1) {
			logDebug "Close threshold met; delaying ${newDelayInactive} seconds"
			unschedule(devActive)
			runIn(newDelayInactive,devInactive)
		} else {
			unschedule(devActive)
			devInactive()
		}
	}
}

def createOrUpdateChildDevice() {
	def childDevice = getChildDevice("contactgroup:" + app.getId())
    if (!childDevice || state.contactDevice == null) {
        logDebug "Creating child device"
		state.contactDevice = "contactgroup:" + app.getId()
		addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "contactgroup:" + app.getId(), 1234, [name: app.label, isComponent: false])
    }
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
            openList.add(it.displayName)
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
	openList = openList.sort()
	openList = groovy.json.JsonOutput.toJson(openList)
	state.openList = openList
    logDebug "There are ${totalClosed} sensors closed"
    logDebug "There are ${totalOpen} sensors open"
    device.sendEvent(name: "TotalClosed", value: totalClosed)
    device.sendEvent(name: "TotalOpen", value: totalOpen)
	device.sendEvent(name: "OpenList", value: state.openList)
}

def devActive() {
    def device = getChildDevice(state.contactDevice)
	log.info "Open threshold met; setting virtual device as open"
	logDebug "Current threshold value is ${activeThreshold}"
	device.sendEvent(name: "contact", value: "open", descriptionText: "The open devices are ${state.openList}")
	device.sendEvent(name: "switch", value: "on")
}

def devInactive() {
	def device = getChildDevice(state.contactDevice)
	log.info "Closed threshold met; setting virtual device as closed"
	logDebug "Current threshold value is ${activeThreshold}"
	device.sendEvent(name: "contact", value: "closed")
	device.sendEvent(name: "switch", value: "off")
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

def getFormat(type, myText="") {
	if(type == "header") return "<div style='color:#660000;font-weight: bold'>${myText}</div>"
}