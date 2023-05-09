/**
 *
 * Sensor Groups+_Humidity
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
    name: "Sensor Groups+_Pressure",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates a virtual device to track a group of pressure sensors.",
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
            childDevice = getChildDevice("pressuregroup:" + app.getId())
            childLink = """<a href="/device/edit/$childDevice.id" style='font-size: 18px'>Click Here To Go To The Child Device</a>"""
            headerTitle="$childLink"
            }

	return dynamicPage(name: "mainPage", uninstall:true, install: true) {
        section("$headerTitle") {}
		section(getFormat("header","Name"),hideable: true, hidden: state.hide) {
            label title: getFormat("importantBold","Enter a name for this child app.")+
            getFormat("lessImportant","<br>This will create a virtual pressure sensor which reports the high, low, and average humidity based on the sensors you select."), required:true,width:4
            paragraph ""
			input "enforceName", "bool", title: getFormat("importantBold","Sync child device name?")+
            getFormat("lessImportant","<br>Enable: the device and app names will sync.<br>Disabled: the device name can be changed."), defaultValue: true, displayDuringSetup: true, required: false, width: 4
		}

		section(getFormat("header","<b>Device Selection</b>")) {
			paragraph getFormat("importantBold","Please choose which sensors to include in this group.")

			input "pressureSensors", "capability.pressureMeasurement", title: getFormat("lessImportant","Devices to monitor"), multiple:true, required:true,width:4
        }

		section(getFormat("header","<b>Options</b>"),hideable: true, hidden: state.hide) {            
            
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
	subscribe(pressureSensors, "pressure", pressureHandler)
    createOrUpdateChildDevice()
    pressureHandler()	
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def pressureHandler(evt) {
    logInfo "Pressure sensor change; checking totals..."
    def totalCount = pressureSensors.size()
    def device = getChildDevice(state.pressureDevice)
    device.sendEvent(name: "TotalCount", value: totalCount)    
    def listPressure = []
    def avgPressure = 0
    def total = 0
    pressureSensors.each {it ->
        def newName = it.displayName
        def map = [:]
        map.name = newName
        map.pressure = it.currentPressure
        map.pressureMbar = it.currentPressure * 10
        total = (total + it.currentPressure)
        listPressure.add(map)
    }
    //Calculate and send average
    avgPressure = (total / totalCount).toDouble().round(1)
    logDebug "Average pressure is ${avgPressure}"
    device.sendEvent(name: "AveragePressure", value: avgPressure, unit: "kPa")
    def avgPressureMbar = avgPressure * 10
    device.sendEvent(name: "AvgPressureMbar", value: avgPressureMbar, unit: "mbar")
    
    //Calculate and send minimum
    minPressure = listPressure.min { it.pressure }
    logDebug "Min pressure is ${minPressure.pressure} from device ${minPressure.name}"
    device.sendEvent(name: "MinPressure", value: minPressure.pressure, unit: "kPa")
    device.sendEvent(name: "MinPressureMbar", value: minPressure.pressureMbar, unit: "mbar")
    device.sendEvent(name: "MinPressureDevice", value:minPressure.name)
    
    //Calculate and send maximum
    maxPressure = listPressure.max { it.pressure }
    logDebug "Max pressure is ${maxPressure.pressure} from device ${maxPressure.name}"
    device.sendEvent(name: "MaxPressureDevice", value:maxPressure.name)
    device.sendEvent(name: "MaxPressureMbar", value: maxPressure.pressureMbar, unit: "mbar")
    device.sendEvent(name: "MaxPressure", value: maxPressure.pressure, unit: "kPa")

    //Create table html string
    String displayList = "<ul style='list-style-type: none; margin: 0;padding: 0'>"
    listPressure.each {it ->
        displayList += "<li>${it.name}: ${it.pressure}</li>"
    }
    displayList += "</ul>"
    device.sendEvent(name: "displayList", value: displayList)
}



def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("pressuregroup:" + app.getId())
    state.pressureDevice = state.pressureDevice ?: "pressuregroup:" + app.getId()
    if (!childDevice) {
        logDebug "Creating child device"
        addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "pressuregroup:" + app.getId(), 1234, [label: app.label, isComponent: false])
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