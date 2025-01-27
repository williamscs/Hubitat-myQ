/**
 *  MyQ Lite
 *
 *  Copyright 2017 Jason Mok/Brian Beaird/Barry Burke/Thomas Howard
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Last Updated : 04/08/2019
 *  SmartApp version: 3.0.2
 *  Door device version: 2.2.5
 *  Door-no-sensor device version: 1.1.9
 *  Light device version: not provided/tested.
 */

definition(
	name: "MyQ Lite",
	namespace: "tchoward",
	author: "Jason Mok/Brian Beaird/Barry Burke/Thomas Howard/Brian Wilson",
	description: "Integrate MyQ with Hubitat",
	category: "SmartThings Labs",
	iconUrl:   "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq.png",
	iconX2Url: "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq@2x.png",
	iconX3Url: "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq@3x.png"
)

preferences {
	page(name: "prefLogIn", title: "MyQ")    
	page(name: "prefListDevices", title: "MyQ")
    page(name: "prefSensor1", title: "MyQ")
    page(name: "prefSensor2", title: "MyQ")
    page(name: "prefSensor3", title: "MyQ")
    page(name: "prefSensor4", title: "MyQ")
    page(name: "prefSensor5", title: "MyQ")
    page(name: "prefSensor6", title: "MyQ")
    page(name: "summary", title: "MyQ")
}


/* Preferences */
def prefLogIn() {
	state.previousVersion = state.thisSmartAppVersion
    if (state.previousVersion == null){
    	state.previousVersion = 0;
    }
    state.thisSmartAppVersion = "3.0.2"
    state.installMsg = ""
    def showUninstall = username != null && password != null 
	return dynamicPage(name: "prefLogIn", title: "Connect to MyQ", nextPage:"prefListDevices", uninstall:showUninstall, install: false) {
		section("Login Credentials"){
			input("username", "email", title: "Username", description: "MyQ Username (email address)")
			input("password", "password", title: "Password", description: "MyQ password")
		}
		section("Gateway Brand"){
			input(name: "brand_t", title: "Gateway Brand", type: "enum", options: [[1:"Liftmaster"],[2:"Chamberlain"],[3:"Craftsman"]] )
		}
        settings.brand = getBrandName(settings.brand_t) 
	}
}

def prefListDevices() {
    getSelectedDevices("lights")
    if (forceLogin()) {
		def doorList = getDoorList()
		if ((state.doorList) || (state.lightList)){
            ifDebug(state.doorList)
        	def nextPage = "prefSensor1"
            if (!state.doorList){nextPage = "summary"}  //Skip to summary if there are no doors to handle            
                return dynamicPage(name: "prefListDevices",  title: "Devices", nextPage:nextPage, install:false, uninstall:true) {
                    if (state.doorList) {
                        section("Select which garage door/gate to use") {
                            input("doors", "enum", required:false, multiple:true, options:state.doorList)
                            input("priority", "enum", title: "Default Message Priority (Blank = NORMAL):", description: "", defaultValue: "0", options:[["-1":"LOW"], ["0":"NORMAL"], ["1":"HIGH"]])

                        }
                    } 
                    if (state.lightList) {
                        section("Select which lights to use"){
                            input(name: "lights", type: "enum", required:false, multiple:true, metadata:[values:state.lightList])
                        }
                    } 
                }
        
        }else {
			def devList = getDeviceList()
			return dynamicPage(name: "prefListDevices",  title: "Error!", install:false, uninstall:true) {
				section(""){
					paragraph "Could not find any supported device(s). Please report to author about these devices: " +  devList
				}
			}
		}
        
          
	} else {
		return dynamicPage(name: "prefListDevices",  title: "Error!", install:false, uninstall:true) {
			section(""){
				paragraph "The username or password you entered is incorrect. Try again. "
			}
		}  
	}
}


def prefSensor1() {
	ifDebug("Doors chosen: " + doors)
    
    //Set defaults
    def nextPage = "summary"    
    def titleText = ""
    
    //Determine if we have multiple doors and need to send to another page
    if (doors instanceof String){ //simulator seems to just make a single door a string. For that reason we have this weird check.
    	ifDebug("Single door detected (string).")
        titleText = "Select Sensors for Door 1 (" + state.data[doors].name + ")"
    }
    else if (doors.size() == 1){
    	ifDebug("Single door detected (array).")
        titleText = "Select Sensors for Door 1 (" + state.data[doors[0]].name + ")"
    }
    else{
    	ifDebug("Multiple doors detected.")
        nextPage = "prefSensor2"
        titleText = "OPTIONAL: Select Sensors for Door 1 (" + state.data[doors[0]].name + ")"        
    }    
    
    return dynamicPage(name: "prefSensor1",  title: "Optional Sensors and Push Buttons", nextPage:nextPage, install:false, uninstall:true) {        
        section(titleText){			
			paragraph "Optional: If you have sensors on this door, select them below. A sensor allows the device type to know whether the door is open or closed, which helps the device function " + 
            	"as a switch you can turn on (to open) and off (to close)."                
            input(name: "door1Sensor", title: "Contact Sensor", type: "capability.contactSensor", required: false, multiple: false)
			input(name: "door1Acceleration", title: "Acceleration Sensor", type: "capability.accelerationSensor", required: false, multiple: false)
		}        
        section("Create separate on/off push buttons?"){			
			paragraph "Choose the option below to have separate additional On and Off push button devices created. This is recommened if you have no sensors but still want a way to open/close the " +
            "garage from SmartTiles and other interfaces like Google Home that can't function with the built-in open/close capability. See wiki for more details."           
            input "prefDoor1PushButtons", "bool", required: false, title: "Create on/off push buttons?"
		}
    }
}

def prefSensor2() {	   
    def nextPage = "summary"    
    def titleText = "Sensors for Door 2 (" + state.data[doors[1]].name + ")"
     
    if (doors.size() > 2){
    	nextPage = "prefSensor3"        
    }
    
    return dynamicPage(name: "prefSensor2",  title: "Optional Sensors and Push Buttons", nextPage:nextPage, install:false, uninstall:true) {
        section(titleText){			
			input(name: "door2Sensor", title: "Contact Sensor", type: "capability.contactSensor", required: false, multiple: false)
			input(name: "door2Acceleration", title: "Acceleration Sensor", type: "capability.accelerationSensor", required: false, multiple: false)
		}
        section("Create separate on/off push buttons?"){
			paragraph "Choose the option below to have extra on and off push button devices created. This is recommened if you have no sensors but still want a way to open/close the garage from SmartTiles."           
            input "prefDoor2PushButtons", "bool", required: false, title: "Create on/off push buttons?"
		}
    }
}

def prefSensor3() {
    def nextPage = "summary"    
    def titleText = "Sensors for Door 3 (" + state.data[doors[2]].name + ")"
     
    if (doors.size() > 3){
    	nextPage = "prefSensor4"        
    }
    
    return dynamicPage(name: "prefSensor3",  title: "Optional Sensors and Push Buttons", nextPage:nextPage, install:false, uninstall:true) {
        section(titleText){			
			input(name: "door3Sensor", title: "Contact Sensor", type: "capability.contactSensor", required: false, multiple: false)
			input(name: "door3Acceleration", title: "Acceleration Sensor", type: "capability.accelerationSensor", required: false, multiple: false)
		}
        section("Create separate on/off push buttons?"){			
			paragraph "Choose the option below to have extra on and off push button devices created. This is recommened if you have no sensors but still want a way to open/close the garage from SmartTiles."           
            input "prefDoor3PushButtons", "bool", required: false, title: "Create on/off push buttons?"
		}
    }
}

def prefSensor4() {	   
    def nextPage = "summary"
	def titleText = "Contact Sensor for Door 4 (" + state.data[doors[3]].name + ")"

    if (doors.size() > 4){
    	nextPage = "prefSensor5"        
    }
    return dynamicPage(name: "prefSensor4",  title: "Optional Sensors and Push Buttons", nextPage:nextPage, install:false, uninstall:true) {
        section(titleText){			
			input(name: "door4Sensor", title: "Contact Sensor", type: "capability.contactSensor", required: "false", multiple: "false")
			input(name: "door4Acceleration", title: "Acceleration Sensor", type: "capability.accelerationSensor", required: false, multiple: false)
		}
        section("Create separate on/off push buttons?"){			
			paragraph "Choose the option below to have extra on and off push button devices created. This is recommened if you have no sensors but still want a way to open/close the garage from SmartTiles."           
            input "prefDoor4PushButtons", "bool", required: false, title: "Create on/off push buttons?"
		}
    }
}

def prefSensor5() {	   
    def nextPage = "summary"
	def titleText = "Contact Sensor for Door 5 (" + state.data[doors[4]].name + ")"
    if (doors.size() > 5){
        nextPage = "prefSensor6"
    }
    return dynamicPage(name: "prefSensor5",  title: "Optional Sensors and Push Buttons", nextPage:nextPage, install:false, uninstall:true) {
        section(titleText){			
			input(name: "door5Sensor", title: "Contact Sensor", type: "capability.contactSensor", required: "false", multiple: "false")
			input(name: "door5Acceleration", title: "Acceleration Sensor", type: "capability.accelerationSensor", required: false, multiple: false)
		}
        section("Create separate on/off push buttons?"){			
			paragraph "Choose the option below to have extra on and off push button devices created. This is recommened if you have no sensors but still want a way to open/close the garage from SmartTiles."           
            input "prefDoor5PushButtons", "bool", required: false, title: "Create on/off push buttons?"
		}
    }
}

def prefSensor6() {	   
    def nextPage = "summary"
	def titleText = "Contact Sensor for Door 6 (" + state.data[doors[5]].name + ")"
    return dynamicPage(name: "prefSensor6",  title: "Optional Sensors and Push Buttons", nextPage:nextPage, install:false, uninstall:true) {
        section(titleText){			
			input(name: "door6Sensor", title: "Contact Sensor", type: "capability.contactSensor", required: "false", multiple: "false")
			input(name: "door6Acceleration", title: "Acceleration Sensor", type: "capability.accelerationSensor", required: false, multiple: false)
		}
        section("Create separate on/off push buttons?"){			
			paragraph "Choose the option below to have extra on and off push button devices created. This is recommened if you have no sensors but still want a way to open/close the garage from SmartTiles."           
            input "prefDoor6PushButtons", "bool", required: false, title: "Create on/off push buttons?"
		}
    }
}

def summary() {	   
	state.installMsg = ""
	state.isDebug = isDebug
    initialize()
    versionCheck()
    return dynamicPage(name: "summary",  title: "Summary", install:true, uninstall:true) {            
        section("Installation Details:"){
			paragraph state.installMsg
            paragraph state.versionWarning
		}
	    section("") {
        	input "isDebug", "bool", title: "Enable Debug Logging", required: false, multiple: false, defaultValue: false, submitOnChange: true
       }
   	}
}

/* Initialization */
def installed() { 	
}

def updated() { 
	ifDebug("Updated...")
}

def uninstalled() {
}	

def initialize() {    
	unsubscribe()
    ifDebug("Initializing...")
    login()
    state.sensorMap = [:]
    
    // Get initial device status in state.data
	state.polling = [ last: 0, rescheduler: now() ]
	state.data = [:]
    
	// Create selected devices
	def doorsList = getDoorList()
	def lightsList = state.lightList
    
    if (doors != null){
        def firstDoor = doors[0]        
        //Handle single door (sometimes it's just a dumb string thanks to the simulator)
        if (doors instanceof String)
        firstDoor = doors   

        //Create door devices
        createChilDevices(firstDoor, door1Sensor, doorsList[firstDoor], prefDoor1PushButtons)
        if (doors[1]) createChilDevices(doors[1], door2Sensor, doorsList[doors[1]], prefDoor2PushButtons)
        if (doors[2]) createChilDevices(doors[2], door3Sensor, doorsList[doors[2]], prefDoor3PushButtons)
        if (doors[3]) createChilDevices(doors[3], door4Sensor, doorsList[doors[3]], prefDoor4PushButtons)    
        if (doors[4]) createChilDevices(doors[4], door4Sensor, doorsList[doors[4]], prefDoor5PushButtons)    
        if (doors[5]) createChilDevices(doors[5], door4Sensor, doorsList[doors[5]], prefDoor6PushButtons)    
    }
    
    //Create light devices
    def selectedLights = getSelectedDevices("lights")    
    selectedLights.each {    
    	ifDebug("Checking for existing light: " + it)
    	if (!getChildDevice(it)) {
        	ifDebug("Creating child light device: " + it)
        	
            try{            
            	addChildDevice("brbeaird", "MyQ Light Controller", it, getHubID(), ["name": lightsList[it]])
                state.installMsg = state.installMsg + lightsList[it] + ": created light device. \r\n\r\n"
            }
            catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
            {
                ifDebug("Error! " + e)                       
                state.installMsg = state.installMsg + lightsList[it] + ": problem creating light device. Check your IDE to make sure the brbeaird : MyQ Light Controller device handler is installed and published. \r\n\r\n"
            }
        }
        else{
        	ifDebug("Light device already exists: " + it)
            state.installMsg = state.installMsg + lightsList[it] + ": light device already exists. \r\n\r\n"
        }
        
    }
    
    // Remove unselected devices
    def selectedDevices = [] + getSelectedDevices("doors") + getSelectedDevices("lights")
    getChildDevices().each{
        //Modify DNI string for the extra pushbuttons to make sure they don't get deleted unintentionally
        def DNI = it?.deviceNetworkId
        DNI = DNI.replace(" Opener", "")
        DNI = DNI.replace(" Closer", "")        

        if (!(DNI in selectedDevices)){
            ifDebug("found device to delete: " + it)
            try{
                	deleteChildDevice(it.deviceNetworkId)
            } catch (e){
                	sendPush("Warning: unable to delete door or button - " + it + "- you'll need to manually remove it.")
                    ifDebug("Error trying to delete device " + it + " - " + e)
                    ifDebug("Device is likely in use in a Routine, or SmartApp (make sure and check SmarTiles!).")
            }
        }
    }
    
    //Create subscriptions
    if (door1Sensor)
        subscribe(door1Sensor, "contact", sensorHandler)
    if (door2Sensor)    	
        subscribe(door2Sensor, "contact", sensorHandler)
    if (door3Sensor)        
        subscribe(door3Sensor, "contact", sensorHandler)        
    if (door4Sensor)    	
        subscribe(door4Sensor, "contact", sensorHandler)
    if (door5Sensor)    	
        subscribe(door5Sensor, "contact", sensorHandler)
    if (door6Sensor)    	
        subscribe(door6Sensor, "contact", sensorHandler)
        
    if (door1Acceleration)
        subscribe(door1Acceleration, "acceleration", sensorHandler)    
    if (door2Acceleration)    	
        subscribe(door2Acceleration, "acceleration", sensorHandler)
    if (door3Acceleration)        
        subscribe(door3Acceleration, "acceleration", sensorHandler)        
    if (door4Acceleration)    	
        subscribe(door4Acceleration, "acceleration", sensorHandler)
    if (door5Acceleration)    	
        subscribe(door5Acceleration, "acceleration", sensorHandler)
    if (door6Acceleration)    	
        subscribe(door6Acceleration, "acceleration", sensorHandler)
        
    //Set initial values
    if (door1Sensor)
    	syncDoorsWithSensors()   
}

def createChilDevices(door, sensor, doorName, prefPushButtons){
	ifDebug("In CreateChild")
    if (door){
        //Has door's child device already been created?
        def existingDev = getChildDevice(door)
        def existingType = existingDev?.typeName
        
        if (existingDev){        
        	ifDebug("Child already exists for " + doorName + ". Sensor name is: " + sensor)
            state.installMsg = state.installMsg + doorName + ": door device already exists. \r\n\r\n"
            if ((!sensor) && existingType == "MyQ Garage Door Opener"){
            	ifDebug("Type needs updating to non-sensor version")
                //existingDev.deviceType = "MyQ Garage Door Opener-NoSensor2"
                state.installMsg = state.installMsg + doorName + ": needs to be changed to No-sensor version. Go remove Sensor Version Driver then re-install." + "\r\n\r\n"
            }
            
            if (sensor && existingType == "MyQ Garage Door Opener-NoSensor"){
            	ifDebug("Type needs updating to sensor version")
                //existingDev.deviceType = "MyQ Garage Door Opener"
                state.installMsg = state.installMsg + doorName + ": needs to be changed to Sensor version. Go remove No-Sensor Version Driver then re-install." + "\r\n\r\n"
            }            
        }
        else{
            ifDebug("Creating child door device " + door)
            
                if (sensor){
                    try{
                        ifDebug("Creating door with sensor")
                        addChildDevice("tchoward", "MyQ Garage Door Opener", door, getHubID(), ["name": doorName]) 
                        state.installMsg = state.installMsg + doorName + ": created door device (sensor version) \r\n\r\n"
                    }
                    catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
                    {
                        ifDebug("Error! " + e)                    
                        state.installMsg = state.installMsg + doorName + ": problem creating door device (sensor type). Check your IDE to make sure the tchoward : MyQ Garage Door Opener device handler is installed and published. \r\n\r\n"
                        
                    }
                }
                else{
                    try{
                        ifDebug("Creating door with no sensor")
                        addChildDevice("tchoward", "MyQ Garage Door Opener-NoSensor", door, getHubID(), ["name": doorName]) 
                        state.installMsg = state.installMsg + doorName + ": created door device (no-sensor version) \r\n\r\n"
                    }
                    catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
                    {
                        ifDebug("Error! " + e)                    
                        state.installMsg = state.installMsg + doorName + ": problem creating door device (no-sensor type). Check your IDE to make sure the tchoward : MyQ Garage Door Opener-NoSensor device handler is installed and published. \r\n\r\n"
                    }
                }
            
        }
        
        //Create push button devices
        if (prefPushButtons){
        	def existingOpenButtonDev = getChildDevice(door + " Opener")
            def existingCloseButtonDev = getChildDevice(door + " Closer")
            if (!existingOpenButtonDev){                
                try{
                	def openButton = addChildDevice("smartthings", "Momentary Button Tile", door + " Opener", getHubID(), [name: doorName + " Opener", label: doorName + " Opener"])
                	state.installMsg = state.installMsg + doorName + ": created push button device. \r\n\r\n"
                	subscribe(openButton, "momentary.pushed", doorButtonOpenHandler)                
                }
                catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
                {
                    ifDebug("Error! " + e)                                         
                    state.installMsg = state.installMsg + doorName + ": problem creating push button device. Check your IDE to make sure the smartthings : Momentary Button Tile device handler is installed and published. \r\n\r\n"
                }
            }
            else{
            	subscribe(existingOpenButtonDev, "momentary.pushed", doorButtonOpenHandler)
                state.installMsg = state.installMsg + doorName + ": push button device already exists. Subscription recreated. \r\n\r\n"
                ifDebug("subscribed to button: " + existingOpenButtonDev)
                
                
                
            }
            
            if (!existingCloseButtonDev){                
                try{
                    def closeButton = addChildDevice("smartthings", "Momentary Button Tile", door + " Closer", getHubID(), [name: doorName + " Closer", label: doorName + " Closer"])
                    subscribe(closeButton, "momentary.pushed", doorButtonCloseHandler)
                }
                catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
                {
                    ifDebug("Error! " + e)                    
                }
            }
            else{
                subscribe(existingCloseButtonDev, "momentary.pushed", doorButtonCloseHandler)                
            }
        }
        
        //Cleanup defunct push button devices if no longer wanted
        else{
        	def pushButtonIDs = [door + " Opener", door + " Closer"]
            ifDebug("ID's to look for: " + pushButtonIDs)
            def devsToDelete = getChildDevices().findAll { pushButtonIDs.contains(it.deviceNetworkId)}
            ifDebug("button devices to delete: " + devsToDelete)
			devsToDelete.each{
            	ifDebug("deleting button: " + it)
                try{
                	deleteChildDevice(it.deviceNetworkId)
                } catch (e){
                	//sendPush("Warning: unable to delete virtual on/off push button - you'll need to manually remove it.")
                    state.installMsg = state.installMsg + "Warning: unable to delete virtual on/off push button - you'll need to manually remove it. \r\n\r\n"
                    ifDebug("Error trying to delete button " + it + " - " + e)
                    ifDebug("Button  is likely in use in a Routine, or SmartApp (make sure and check SmarTiles!).")
                }
            	
            }
        } 	
    }
}


def syncDoorsWithSensors(child){	
    def firstDoor = doors[0]
        
    //Handle single door (sometimes it's just a dumb string thanks to the simulator)
    if (doors instanceof String)
    firstDoor = doors
    
    def doorDNI = null
    if (child) {								// refresh only the requesting door (makes things a bit more efficient if you have more than 1 door
    	doorDNI = child.device.deviceNetworkId
        switch (doorDNI) {
        	case firstDoor:
            	updateDoorStatus(firstDoor, door1Sensor, door1Acceleration, door1ThreeAxis, child)
                break
            case doors[1]:
            	updateDoorStatus(doors[1], door2Sensor, door2Acceleration, door2ThreeAxis, child)
                break
            case doors[2]:
            	updateDoorStatus(doors[2], door3Sensor, door3Acceleration, door3ThreeAxis, child)
                break
            case doors[3]:
            	updateDoorStatus(doors[3], door4Sensor, door4Acceleration, door4ThreeAxis, child)
				break
            case doors[4]:
            	updateDoorStatus(doors[4], door5Sensor, door5Acceleration, door5ThreeAxis, child)
				break
            case doors[5]:
            	updateDoorStatus(doors[5], door6Sensor, door6Acceleration, door6ThreeAxis, child)
     	}
    } else {           					// refresh ALL the doors
		if (firstDoor) updateDoorStatus(firstDoor, door1Sensor, door1Acceleration, door1ThreeAxis, null)
		if (doors[1]) updateDoorStatus(doors[1], door2Sensor, door2Acceleration, door2ThreeAxis, null)
		if (doors[2]) updateDoorStatus(doors[2], door3Sensor, door3Acceleration, door3ThreeAxis, null)
		if (doors[3]) updateDoorStatus(doors[3], door4Sensor, door4Acceleration, door4ThreeAxis, null)
		if (doors[4]) updateDoorStatus(doors[4], door5Sensor, door5Acceleration, door5ThreeAxis, null)
		if (doors[5]) updateDoorStatus(doors[5], door6Sensor, door6Acceleration, door6ThreeAxis, null)
    }
}

def updateDoorStatus(doorDNI, sensor, acceleration, threeAxis, child){
	
    //Get door to update and set the new value
    def doorToUpdate = getChildDevice(doorDNI)
    def doorName = state.data[doorDNI].name
    
    def value = "unknown"
    def moving = "unknown"
    def door = doorToUpdate.latestValue("door")
    
    if (acceleration) moving = acceleration.latestValue("acceleration")
    if (sensor) value = sensor.latestValue("contact")

    if (moving == "active") {
    	if (value == "open") {			
        	if (door != "opening") value = "closing" else value = "opening"  // if door is "open" or "waiting" change to "closing", else it must be "opening"
    	} else if (value == "closed") { 
        	if (door != "closing") 	value = "opening" else value = "closed"
    	}
    } else if (moving == "inactive") {
    	if (door == "closing") {
    		if (value == "open") { 	// just stopped but door is still open
    			value = "stopped"
        	}
        }
    }
    	
    doorToUpdate.updateDeviceStatus(value)
    doorToUpdate.updateDeviceSensor("${sensor} is ${sensor?.currentContact}")
    
    ifDebug("Door: " + doorName + ": Updating with status - " + value + " -  from sensor " + sensor)
    
     if (acceleration) {
     	doorToUpdate.updateDeviceMoving("${acceleration} is ${moving}")
        ifDebug("Door: " + doorName + ": Updating with status - " + moving + " - from sensor " + acceleration)
     }
    
    //Get latest activity timestamp for the sensor (data saved for up to a week)
    def eventsSinceYesterday = sensor.eventsSince(new Date() - 7)    
    def latestEvent = eventsSinceYesterday[0]?.date
    def timeStampLogText = "Door: " + doorName + ": Updating timestamp to: " + latestEvent + " -  from sensor " + sensor
    
    if (!latestEvent)	//If the door has been inactive for more than a week, timestamp data will be null. Keep current value in that case.
    	timeStampLogText = "Door: " + doorName + ": Null timestamp detected "  + " -  from sensor " + sensor + " . Keeping current value."
    else
    	doorToUpdate.updateDeviceLastActivity(latestEvent)    	
    	
    ifDebug(timeStampLogText)  
}

def refresh(child){	
    def door = child.device.deviceNetworkId
    def doorName = state.data[door].name
    ifDebug("refresh called from " + doorName + ' (' + door + ')')
    syncDoorsWithSensors(child)
}

def sensorHandler(evt) {    
    ifDebug("Sensor change detected: Event name  " + evt.name + " value: " + evt.value   + " deviceID: " + evt.deviceId)
    
	//If we're seeing vibration sensor values, ignore them if it's been more than 30 seconds after a command was sent.
    // This keeps us from seeing phantom entries from overly-sensitive sensors
	if (evt.value == "active" || evt.value == "inactive"){
		if (state.lastCommandSent == null || state.lastCommandSent > now()-30000){
    		return 0;
    	}
	}   
    
    switch (evt.deviceId) {
    	case door1Sensor.id:
        case door1Acceleration?.id:
            def firstDoor = doors[0]
			if (doors instanceof String) firstDoor = doors
        	updateDoorStatus(firstDoor, door1Sensor, door1Acceleration, door1ThreeAxis, null)
            break
    	case door2Sensor?.id:
        case door2Acceleration?.id:
        	updateDoorStatus(doors[1], door2Sensor, door2Acceleration, door2ThreeAxis, null)
            break    	
        case door3Sensor?.id:
        case door3Acceleration?.id:
        	updateDoorStatus(doors[2], door3Sensor, door3Acceleration, door3ThreeAxis, null)
            break        
    	case door4Sensor?.id:
        case door4Acceleration?.id:
        	updateDoorStatus(doors[3], door4Sensor, door4Acceleration, door4ThreeAxis, null)
            break
    	case door5Sensor?.id:
        case door5Acceleration?.id:
        	updateDoorStatus(doors[4], door5Sensor, door5Acceleration, door5ThreeAxis, null)
            break
    	case door6Sensor?.id:
        case door6Acceleration?.id:
        	updateDoorStatus(doors[5], door6Sensor, door6Acceleration, door6ThreeAxis, null)
            break
        default:
			syncDoorsWithSensors()
    }
}

def doorButtonOpenHandler(evt) {
    ifDebug("Door open button push detected: Event name  " + evt.name + " value: " + evt.value   + " deviceID: " + evt.deviceId + " DNI: " + evt.getDevice().deviceNetworkId)
    def doorDeviceDNI = evt.getDevice().deviceNetworkId
    doorDeviceDNI = doorDeviceDNI.replace(" Opener", "")
    def doorDevice = getChildDevice(doorDeviceDNI)
    ifDebug("Opening door.")
    doorDevice.openPrep()
    sendCommand(doorDevice, "desireddoorstate", 1) 
}

def doorButtonCloseHandler(evt) {    
   ifDebug("Door close button push detected: Event name  " + evt.name + " value: " + evt.value   + " deviceID: " + evt.deviceId + " DNI: " + evt.getDevice().deviceNetworkId)
    def doorDeviceDNI = evt.getDevice().deviceNetworkId
    doorDeviceDNI = doorDeviceDNI.replace(" Closer", "")
	def doorDevice = getChildDevice(doorDeviceDNI)
    ifDebug("Closing door.")
    doorDevice.closePrep()
    sendCommand(doorDevice, "desireddoorstate", 0) 
}


def getSelectedDevices( settingsName ) { 
	def selectedDevices = [] 
	(!settings.get(settingsName))?:((settings.get(settingsName)?.getAt(0)?.size() > 1)  ? settings.get(settingsName)?.each { selectedDevices.add(it) } : selectedDevices.add(settings.get(settingsName))) 
	return selectedDevices 
} 

private getBrandName(val){
    switch (val) {
     	case 1: 
        	return "Liftmaster"
        	break
        case 2:
        	return "Chamberlain"
        	break
        case 3:
        	return "Craftsman"
        	break
    }
}

/* Access Management */
private forceLogin() {
    
	//Reset token and expiry
	state.session = [ brandID: 0, brandName: settings.brand, securityToken: null, expiration: 0 ]
	state.polling = [ last: 0, rescheduler: now() ]
	state.data = [:]
    def a = doLogin();
    ifDebug('a: ' + a)
	return a;
}

private login() { return (!(state.session.expiration > now())) ? doLogin() : true }

private doLogin() { 
    ifDebug(doLoginStart)
    def success = false;
    apiPostLogin("/api/v4/User/Validate", [ username: settings.username, password: settings.password ]) { response ->	
		ifDebug("got login response: " + response + " : " + response.data.SecurityToken+" :: "+response.status)
        if (response.status == 200) {
			if (response.data.SecurityToken != null) {
				//state.session.brandID = response.data.BrandId
				//state.session.brandName = response.data.BrandName
				state.session.securityToken = response.data.SecurityToken
				state.session.expiration = now() + 150000
                ifDebug("doLoginDone")
                success = true;
				//return true
			} else {
				//return false
			}
		} else {
			//return false
		}
	}
   	return success;
}

// Listing all the garage doors you have in MyQ
private getDoorList() { 	    
	state.doorList = [:]
    state.lightList = [:]
    
    def deviceList = [:]
	apiGet("/api/v4/userdevicedetails/get", []) { response ->
		if (response.status == 200) {
            //sendAlert("response data: " + response.data.Devices)
			response.data.Devices.each { device ->
				// 2 = garage door, 5 = gate, 7 = MyQGarage(no gateway), 17 = Garage Door Opener WGDO
				if (device.MyQDeviceTypeId == 2||device.MyQDeviceTypeId == 5||device.MyQDeviceTypeId == 7||device.MyQDeviceTypeId == 17) {
					ifDebug("Found door: " + device.MyQDeviceId)
                    def dni = [ app.id, "GarageDoorOpener", device.MyQDeviceId ].join('|')
					def description = ''
                    def doorState = ''
                    def updatedTime = ''
                    device.Attributes.each { 
						
                        if (it.AttributeDisplayName=="desc")	//deviceList[dni] = it.Value                        	
                        {
                        	description = it.Value
                        }
                        	
						if (it.AttributeDisplayName=="doorstate") { 
                        	doorState = it.Value
                            updatedTime = it.UpdatedTime
						}
					}
                    
                    
                    //Sometimes MyQ has duplicates. Check and see if we've seen this door before
                        def doorToRemove = ""
                        state.data.each { doorDNI, door ->
                        	if (door.name == description){
                            	ifDebug("Duplicate door detected. Checking to see if this one is newer...")
                                
                                //If this instance is newer than the duplicate, pull the older one back out of the array
                                if (door.lastAction < updatedTime){	
                                	ifDebug("Yep, this one is newer.")
                                    doorToRemove = door 
                                }
                                
                                //If this door is the older one, clear out the description so it will be ignored
                                else{
                                	ifDebug("Nope, this one is older. Stick with what we've got.")
                                    description = ""
                                }
                            }
                        }                        
                        if (doorToRemove){
                        	ifDebug("Removing older duplicate.")
                            state.data.remove(door)
                            state.doorList.remove(door)
                        }                    
                    
                    //Ignore any doors with blank descriptions
                    if (description != ''){
                        ifDebug("Storing door info: " + description + "type: " + device.MyQDeviceTypeId + " status: " + doorState +  " type: " + device.MyQDeviceTypeName)
                        deviceList[dni] = description
                        state.doorList[dni] = description
                        state.data[dni] = [ status: doorState, lastAction: updatedTime, name: description, type: device.MyQDeviceTypeId ]
                    }
                    else{
                    	ifDebug("Door " + device.MyQDeviceId + " has blank desc field. This is unusual...")
                    }
				}
                
                //Lights!
                if (device.MyQDeviceTypeId == 3) {
					ifDebug("Found light: " + device.MyQDeviceId)
                    def dni = [ app.id, "LightController", device.MyQDeviceId ].join('|')
					def description = ''
                    def lightState = ''
                    def updatedTime = ''
                    device.Attributes.each { 
						
                        if (it.AttributeDisplayName=="desc")	//deviceList[dni] = it.Value                        	
                        {
                        	description = it.Value
                        }
                        	
						if (it.AttributeDisplayName=="lightstate") { 
                        	lightState = it.Value
                            updatedTime = it.UpdatedTime							
						}
					}
                    
                    //Ignore any lights with blank descriptions
                    if (description != ''){
                        ifDebug("Storing light info: " + description + "type: " + device.MyQDeviceTypeId + " status: " + doorState +  " type: " + device.MyQDeviceTypeName)
                        deviceList[dni] = description
                        state.lightList[dni] = description
                        state.data[dni] = [ status: lightState, lastAction: updatedTime, name: description, type: device.MyQDeviceTypeId ]
                    }
				}
			}
		}
	}

	return deviceList
}

private getDeviceList() { 	    
	def deviceList = []
	apiGet("/api/v4/userdevicedetails/get", []) { response ->
		if (response.status == 200) {
			response.data.Devices.each { device ->
				ifDebug("MyQDeviceTypeId : " + device.MyQDeviceTypeId.toString())
				if (!(device.MyQDeviceTypeId == 1||device.MyQDeviceTypeId == 2||device.MyQDeviceTypeId == 3||device.MyQDeviceTypeId == 5||device.MyQDeviceTypeId == 7)) {					
                    device.Attributes.each { 
						def description = ''
                        def doorState = ''
                        def updatedTime = ''
                        if (it.AttributeDisplayName=="desc")	//deviceList[dni] = it.Value
                        	description = it.Value
						
                        //Ignore any doors with blank descriptions
                        if (description != ''){
                        	ifDebug("found device: " + description)
                        	deviceList.add( device.MyQDeviceTypeId.toString() + "|" + device.TypeID )
                        }
					}
				}
			}
		}
	}
    
	return deviceList
}

def getHubID(){
    //def hubID    
    //def hubs = location.hubs.findAll{ it.type == com.hubitat.device.HubType.PHYSICAL } 
    //log.debug "hub count: ${hubs.size()}"
    //log.debug "hubID: ${hubID}"
    return 1234//hubs[0].id 
}


/* api connection */
// get URL 
private getApiURL() {
	if (settings.brand == "Craftsman") {
		return "https://craftexternal.myqdevice.com"
	} else {
		return "https://myqexternal.myqdevice.com"
	}
}

private getApiAppID() {
	if (settings.brand == "Craftsman") {
		return "eU97d99kMG4t3STJZO/Mu2wt69yTQwM0WXZA5oZ74/ascQ2xQrLD/yjeVhEQccBZ"
	} else {
		return "NWknvuBd7LoFHfXmKNMBcgajXtZEgKUh4V7WNzMidrpUUluDpVYVZx+xT4PCM5Kx"
	}
}
	
// HTTP GET call
private apiGet(apiPath, apiQuery = [], callback = {}) {	
	// set up query
    def myHeaders = ""
    
    
	apiQuery = [ appId: getApiAppID() ] + apiQuery
	if (state.session.securityToken) { 
    	apiQuery = apiQuery + [SecurityToken: state.session.securityToken ] 
        myHeaders = [ "SecurityToken": state.session.securityToken,                        
                         "MyQApplicationId": getApiAppID() ]
        }
       
	try {
		httpGet([ uri: getApiURL(), path: apiPath, headers: myHeaders, query: apiQuery ]) { response -> callback(response) }
	}	catch (SocketException e)	{
		//sendAlert("API Error: $e")
        ifDebug("API Error: $e")
	}
}

// HTTP PUT call
private apiPut(apiPath, apiBody = [], callback = {}) {    
	// set up body
	apiBody = [ ApplicationId: getApiAppID() ] + apiBody
	if (state.session.securityToken) { apiBody = apiBody + [SecurityToken: state.session.securityToken ] }
    
	def myHeaders = ""  
    
    // set up query
	def apiQuery = [ appId: getApiAppID() ]
	if (state.session.securityToken) { 
    	apiQuery = apiQuery + [SecurityToken: state.session.securityToken ]
        myHeaders = [ "SecurityToken": state.session.securityToken,                        
                         "MyQApplicationId": getApiAppID() ]
    }
    
	try {
		httpPut([ uri: getApiURL(), path: apiPath, headers: myHeaders, contentType: "application/json; charset=utf-8", body: apiBody, query: apiQuery ]) { response -> callback(response) }
	} catch (SocketException e)	{
		ifDebug("API Error: $e")
	}
}

// HTTP POST call
private apiPostLogin(apiPath, apiBody = [], callback = {}) {    
	// set up body
	apiBody = apiBody
    def myHeaders = [ "User-Agent": "Chamberlain/3.73",
                        "BrandId": "2",
                         "ApiVersion": "4.1",
                         "Culture": "en",
                         "MyQApplicationId": getApiAppID() ]
	try {
		httpPost([ uri: getApiURL(), path: apiPath, headers: myHeaders, contentType: "application/json; charset=utf-8", body: apiBody ]) { response -> callback(response) }
	} catch (SocketException e)	{
		ifDebug("API Error: $e")
	}
}

// Get Device ID
def getChildDeviceID(child) {
	return child.device.deviceNetworkId.split("\\|")[2]
}

// Get single device status
def getDeviceStatus(child) {
	return state.data[child.device.deviceNetworkId].status
}

// Get single device last activity
def getDeviceLastActivity(child) {
	return state.data[child.device.deviceNetworkId].lastAction.toLong()
}

// Send command to start or stop
def sendCommand(child, attributeName, attributeValue) {
	state.lastCommandSent = now()
    if (login()) {
		//Send command
		apiPut("/api/v4/deviceattribute/putdeviceattribute", [ MyQDeviceId: getChildDeviceID(child), AttributeName: attributeName, AttributeValue: attributeValue ]) 
        
        if ((attributeName == "desireddoorstate") && (attributeValue == 0)) {		// if we are closing, check if we have an Acceleration sensor, if so, "waiting" until it moves
            def firstDoor = doors[0]
    		if (doors instanceof String) firstDoor = doors
        	def doorDNI = child.device.deviceNetworkId
        	switch (doorDNI) {
        		case firstDoor:
                	if (door1Sensor){if (door1Acceleration) child.updateDeviceStatus("waiting") else child.updateDeviceStatus("closing")}
                	break
            	case doors[1]:
            		if (door2Sensor){if (door2Acceleration) child.updateDeviceStatus("waiting") else child.updateDeviceStatus("closing")}
                	break
            	case doors[2]:
            		if (door3Sensor){if (door3Acceleration) child.updateDeviceStatus("waiting") else child.updateDeviceStatus("closing")}
                	break
            	case doors[3]:
            		if (door4Sensor){if (door4Acceleration) child.updateDeviceStatus("waiting") else child.updateDeviceStatus("closing")}
        			break
            	case doors[4]:
            		if (door5Sensor){if (door5Acceleration) child.updateDeviceStatus("waiting") else child.updateDeviceStatus("closing")}
        			break
            	case doors[5]:
            		if (door6Sensor){if (door6Acceleration) child.updateDeviceStatus("waiting") else child.updateDeviceStatus("closing")}
        			break
            }
        }      
		return true
	} 
}

def versionCheck(){
	state.versionWarning = ""    
    state.thisDoorVersion = ""
	state.thisDoorNoSensorVersion = ""
    state.thisLightVersion = ""
    state.versionWarning = ""
    
    def usesDoorDev = false
    def usesDoorNoSensorDev = false
    def usesLightControllerDev = false
    
    getChildDevices().each { childDevice ->
    	
        try {			
            def devType = childDevice.getTypeName()            
            
            if (devType != "Momentary Button Tile"){               	                
                if (devType == "MyQ Garage Door Opener"){
                	usesDoorDev = true
                    state.thisDoorVersion = childDevice.showVersion()                    
                }
                if (devType == "MyQ Garage Door Opener-NoSensor"){
                	usesDoorNoSensorDev = true
                    state.thisDoorNoSensorVersion = childDevice.showVersion()                    
                }
                if (devType == "MyQ Light Controller"){                
                	usesLightControllerDev = true
                    state.thisLightVersion = childDevice.showVersion()                    
                }
            }
            
		} catch (MissingPropertyException e)	{
			ifDebug("API Error: $e")
		}
    }

    ifDebug(state.versionWarning)
}

private ifDebug(msg) {  
    if (msg && state.isDebug)  log.debug 'MyQ Lite: ' + msg  
}

