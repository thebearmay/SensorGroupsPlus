/**
 *
 * Sensor Groups+_Motion
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
    name: "Sensor Groups+_Motion",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates a virtual device to track a group of motion sensors.",
    category: "Convenience",
    parent: "rle.sg+:Sensor Groups+",
    iconUrl: "",
    iconX2Url: "")

preferences {
    page(name: "mainPage")
}

def mainPage() {
    if(app.getInstallationState() != "COMPLETE") {hide=false} else {hide=true}

	return dynamicPage(name: "mainPage", uninstall:true, install: true) {
		section(getFormat("header","Name"),hideable: true, hidden: hide) {
            label title: getFormat("importantBold","Enter a name for this child app.")+
            getFormat("lessImportant","<br>This will create a virtual motion sensor which reports the active/inactive status based on the sensors you select."), required:true,width:4
            paragraph ""
			input "enforceName", "bool", title: getFormat("importantBold","Sync child device name?")+
            getFormat("lessImportant","<br>Enable: the device and app names will sync.<br>Disabled: the device name can be changed."), defaultValue: true, displayDuringSetup: true, required: false, width: 4
		}

		section(getFormat("header","<b>Device Selection</b>")) {
			paragraph getFormat("importantBold","Please choose which sensors to include in this group.")

			input "motionSensors", "capability.motionSensor", title: getFormat("lessImportant","Motion devices to monitor"), multiple:true, required:true,width:4
            input "accelSensors", "capability.accelerationSensor", title: getFormat("lessImportant","Acceleration devices to monitor (Optional)"), multiple:true, required:false,width:4
        }

		section(getFormat("header","<b>Options</b>"),hideable: true, hidden: hide) {            
            input "activeThreshold", "number", title: getFormat("importantBold","How many sensors must be active before the group is active?")+
				getFormat("lessImportant","<br>Leave set to one if any motion sensor active should make the group active."), required:false, defaultValue: 1,width:4
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
	subscribe(motionSensors, "motion", motionHandler)
    subscribe(accelSensors, "acceleration", motionHandler)
    createOrUpdateChildDevice()
    def device = getChildDevice(state.motionDevice)
    getCurrentCount()
    if (state.totalActive >= activeThreshold) {devActive()} else {devInactive()}
    def totalCount = motionSensors.size()
    if(accelSensors) {totalCount = totalCount + accelSensors.size()}
	device.sendEvent(name: "TotalCount", value: totalCount)
    device.sendEvent(name: "ActiveThreshold", value: activeThreshold) 
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def motionHandler(evt) {
    logDebug "Motion sensor change; checking totals..."
    def device = getChildDevice(state.motionDevice)
    getCurrentCount()
    def newDelayActive = delayActive ?: 0
	def newDelayInactive = delayInactive ?: 0
    if (state.totalActive >= activeThreshold) 
    {
		if(newDelayActive >= 1) {
			logDebug "Active threshold met; delaying ${newDelayActive} seconds"
			unschedule(devInactive)
			runIn(newDelayActive,devActive,[overwrite:false])
		} else {
            unschedule(devInactive)
			devActive()
		}
	} else {
		if(newDelayInactive >= 1) {
			logDebug "Active threshold not met; delaying ${newDelayInactive} seconds"
			unschedule(devActive)
			runIn(newDelayInactive,devInactive)
		} else {
            unschedule(devActive)
			devInactive()
		}
	}
}


def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("motiongroup:" + app.getId())
    if (!childDevice || state.motionDevice == null) {
        logDebug "Creating child device"
        state.motionDevice = "motiongroup:" + app.getId()
        addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "motiongroup:" + app.getId(), 1234, [label: app.label, isComponent: false])
    }
    if(enforceName) {
		if(childDevice.displayName != app.label) {
			childDevice.label = app.label
		}
	}
}

def getCurrentCount() {
    def device = getChildDevice(state.motionDevice)
    def totalActive = 0
    def totalInactive = 0
    def activeList = []
    motionSensors.each { it ->
        if (it.currentValue("motion") == "active") 
        {
            totalActive++
            activeList.add(it.displayName)
        } 
        else if (it.currentValue("motion") == "inactive") 
        {
			totalInactive++
		}
    }
    if(accelSensors) {accelSensors.each { it ->
        if (it.currentValue("acceleration") == "active") 
        {
            totalActive++
            activeList.add(it.displayName)
        } 
        else if (it.currentValue("acceleration") == "inactive") 
        {
			totalInactive++
		}
    }}
    state.totalActive = totalActive
    if (activeList.size() == 0) {
        activeList.add("None")
    }
    activeList = activeList.sort()
    state.activeList = activeList
    logDebug "There are ${totalActive} sensors active."
    logDebug "There are ${totalInactive} sensors inactive."
    device.sendEvent(name: "TotalActive", value: totalActive)
    device.sendEvent(name: "TotalInactive", value: totalInactive)
    device.sendEvent(name: "ActiveList", value: state.activeList)

    //Create display list
    String displayList = "<ul style='list-style-type: none; margin: 0;padding: 0'>"
	activeList.each {it ->
	displayList += "<li>${it}</li>"
	}
	displayList += "</ul>"
	device.sendEvent(name: "displayList", value: displayList)
}

def devActive() {
    def device = getChildDevice(state.motionDevice)
    logInfo "Active threshold met; setting group device as active"
    logDebug "Current threshold value is ${activeThreshold}"
    device.sendEvent(name: "motion", value: "active", descriptionText: "The detected devices are ${state.activeList}")
}

def devInactive() {
	def device = getChildDevice(state.motionDevice)
	logInfo "Active threshold not met; setting group device as inactive"
    logDebug "Current threshold value is ${activeThreshold}"
    device.sendEvent(name: "motion", value: "inactive")
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
	if(type == "red") return "<div style='color:#660000'>${myText}</div>"
	if(type == "importantBold") return "<div style='color:#32a4be;font-weight: bold'>${myText}</div>"
	if(type == "important") return "<div style='color:#32a4be'>${myText}</div>"
	if(type == "important2") return "<div style='color:#5a8200'>${myText}</div>"
	if(type == "important2Bold") return "<div style='color:#5a8200;font-weight: bold'>${myText}</div>"
	if(type == "lessImportant") return "<div style='color:green'>${myText}</div>"
	if(type == "rateDisplay") return "<div style='color:green; text-align: center;font-weight: bold'>${myText}</div>"
}