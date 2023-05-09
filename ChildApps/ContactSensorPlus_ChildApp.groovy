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
 * v1.0		RLE		See parent for changelog
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
	if(app.getInstallationState() != "COMPLETE") {state.hide=false} else {state.hide=true}
    String headerTitle
    if(app.getInstallationState() != "COMPLETE") {
        headerTitle=getFormat("headerHuge","A child device link will appear here after install!")
        } else {
            childDevice = getChildDevice(state.contactDevice)
            childLink = """<a href="/device/edit/$childDevice.id" style='font-size: 18px'>Click Here To Go To The Child Device</a>"""
            headerTitle="$childLink"
            }

	return dynamicPage(name: "mainPage", uninstall:true, install: true) {
        section("$headerTitle") {}
		section(getFormat("header","Name"),hideable: true, hidden: state.hide) {
            label title: getFormat("importantBold","Enter a name for this child app.")+
            getFormat("lessImportant","<br>This app will also create a virtual contact sensor which reports the open/closed status based on the sensors you select."), required:true,width:4
			paragraph ""
			input "enforceName", "bool", title: getFormat("importantBold","Sync child device name?")+
            getFormat("lessImportant","<br>Enable: the device and app names will sync.<br>Disabled: the device name can be changed."), defaultValue: true, displayDuringSetup: true, required: false, width: 4
		}

		section(getFormat("header","<b>Device Selection</b>")) {
			paragraph getFormat("importantBold","Please choose which sensors to include in this group.")

			input "contactSensors", "capability.contactSensor", title: getFormat("lessImportant","Devices to monitor"), multiple:true, required:true,width:4
        }

		section(getFormat("header","<b>Options</b>"),hideable: true, hidden: state.hide) {            
            input "activeThreshold", "number", title: getFormat("importantBold","How many sensors must be open before the group is open?")+
			getFormat("lessImportant","<br>Leave set to one if any contact sensor open should make the group open."), required:false, defaultValue: 1,width:4
			paragraph ""
			input "delayActive", "number", title: getFormat("importantBold","Add a delay before activating the group device?")+
			getFormat("lessImportant","<br>Optional, in seconds"), required:false,width:4
			input "delayInactive", "number", title: getFormat("importantBold","<b>Add a delay before deactivating the group device?</b>")+
			getFormat("lessImportant","<br>Optional, in seconds"), required:false,width:4
			paragraph ""
			input "infoOutput", "bool", title: "Enable info logging?", defaultValue: true, displayDuringSetup: false, required: false, width: 2
            input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false, width: 2
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
			runIn(newDelayActive,devActive,[overwrite:false])
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
	state.contactDevice = state.contactDevice ?: "contactgroup:" + app.getId()
    if (!childDevice) {
        logDebug "Creating child device"
		addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "contactgroup:" + app.getId(), 1234, [label: app.label, isComponent: false])
    }
	if(state.hide) {
		if(enforceName) {
			if(childDevice.displayName != app.label) {
				log.warn "Child device of: ${childDevice.displayName} does not equal app name of: ${app.label}. Resetting device name."
				childDevice.label = app.label
			}
		}
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
	state.openList = openList
    logDebug "There are ${totalClosed} sensors closed"
    logDebug "There are ${totalOpen} sensors open"
    device.sendEvent(name: "TotalClosed", value: totalClosed)
    device.sendEvent(name: "TotalOpen", value: totalOpen)
	device.sendEvent(name: "OpenList", value: openList)

    //Create display list
    String displayList = "<ul style='list-style-type: none; margin: 0;padding: 0'>"
	openList.each {it ->
	displayList += "<li>${it}</li>"
	}
	displayList += "</ul>"
	device.sendEvent(name: "displayList", value: displayList)
}

def devActive() {
    def device = getChildDevice(state.contactDevice)
	logInfo "Open threshold met; setting ${device.displayName} as open"
	logDebug "Current threshold value is ${activeThreshold}"
	device.sendEvent(name: "contact", value: "open", descriptionText: "The open devices are ${state.openList}")
	device.sendEvent(name: "switch", value: "on")
}

def devInactive() {
	def device = getChildDevice(state.contactDevice)
	logInfo "Closed threshold met; setting ${device.displayName} as closed"
	logDebug "Current threshold value is ${activeThreshold}"
	device.sendEvent(name: "contact", value: "closed")
	device.sendEvent(name: "switch", value: "off")
}

def logInfo(msg) {
    if (settings?.infoOutput) {
		log.info msg
    }
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
    if(type == "headerHuge") return "<div style='color:#5a8200;font-weight: bold;font-size:20px'>${myText}</div>"
	if(type == "red") return "<div style='color:#660000'>${myText}</div>"
	if(type == "importantBold") return "<div style='color:#32a4be;font-weight: bold'>${myText}</div>"
	if(type == "important") return "<div style='color:#32a4be'>${myText}</div>"
	if(type == "important2") return "<div style='color:#5a8200'>${myText}</div>"
	if(type == "important2Bold") return "<div style='color:#5a8200;font-weight: bold'>${myText}</div>"
	if(type == "lessImportant") return "<div style='color:green'>${myText}</div>"
	if(type == "rateDisplay") return "<div style='color:green; text-align: center;font-weight: bold'>${myText}</div>"
}