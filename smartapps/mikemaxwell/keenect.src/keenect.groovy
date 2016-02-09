/**
 *  Keenect 0.1.7
 	 
    0.1.7 	removed vo reporting
    		re-wrote switch handler
            removed close vent option
    0.1.6	released vo reporting
 	0.1.5a	fixed quick recovery causing zone to bypass setback detection
    0.1.5	added quick recovery support
 	0.1.3	vent close global options changed
    0.1.2a	update for todays change in todays map input change
    0.1.2	0.1.1 was a cruel and mean thing...
    0.1.1	fixed delay notification and null init issues
    0.1.0	detected setback 
    		force vent vo option, one time on page change, sets all zone vents to the selected option
    0.0.8a	fixed initial notify delay bug
    		moved vent polling to child
    0.0.8	other stuff to support the needs of the children
 	0.0.7	added fan run on delay
 *
 *  Copyright 2015 Mike Maxwell
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
 */
 
definition(
    name		: "Keenect",
    namespace	: "MikeMaxwell",
    author		: "Mike Maxwell",
    description	: "Keen Vent Manager",
    category	: "My Apps",
    iconUrl		: "https://raw.githubusercontent.com/MikeMaxwell/smartthings/master/keen-app-icon.png",
    iconX2Url	: "https://raw.githubusercontent.com/MikeMaxwell/smartthings/master/keen-app-icon.png"
)

preferences {
	page(name: "main")
    page(name: "reporting")
    page(name: "report")
    page(name: "advanced")
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	//initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	state.vParent = "0.1.7"
    //subscribe(tStat, "thermostatSetpoint", notifyZones) doesn't look like we need to use this
    subscribe(tStat, "thermostatMode", checkNotify)
    subscribe(tStat, "thermostatFanMode", checkNotify)
    subscribe(tStat, "thermostatOperatingState", checkNotify)
    subscribe(tStat, "heatingSetpoint", checkNotify)
    subscribe(tStat, "coolingSetpoint", checkNotify)
    //tempSensors
    subscribe(tempSensors, "temperature", checkNotify)

	//init state vars
	state.mainState = state.mainState ?: getNormalizedOS(tStat.currentValue("thermostatOperatingState"))
    state.mainMode = state.mainMode ?: getNormalizedOS(tStat.currentValue("thermostatMode"))
    state.mainCSP = state.mainCSP ?: tStat.currentValue("coolingSetpoint").toFloat()
    state.mainHSP = state.mainHSP ?: tStat.currentValue("heatingSetpoint").toFloat()
    state.mainTemp = state.mainTemp ?: tempSensors.currentValue("temperature").toFloat()
    //state.mainQuick	 = state.mainQuick ?: false
    checkNotify(null)
    //log.debug "app.id:${app.id}" app.id:2442be54-1cbc-4fe8-a378-baaffdf06591
  	state.etf = app.id == '2442be54-1cbc-4fe8-a378-baaffdf06591'
    
}

/* page methods	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
def main(){
	def installed = app.installationState == "COMPLETE"
	return dynamicPage(
    	name		: "main"
        ,title		: "Zones"
        ,install	: true
        ,uninstall	: installed
        ){	if (installed){
        		section(){
        			app(name: "childZones", appName: "keenectZone", namespace: "MikeMaxwell", description: "Create New Vent Zone...", multiple: true)	
                }
           		section("Reporting"){
         			href( "reporting"
						,title		: "Available reports..."
						,description: ""
						,state		: null
					)                
                }
                section("Advanced"){
                	def afTitle = "Advanced features:"
                	def afDesc = '\n\tLog level is ' + getLogLevel(settings.logLevel) + '\n\t' + (sendEventsToNotifications ?  "Notification feed is [on]" : "Notification feed is [off]")
					href( "advanced"
						,title			: afTitle
						,description	: afDesc
						,state			: null
					)
                }                
             }
		     section("Configuration"){
                   	input(
                        name			: "tStat"
                        ,title			: "Main Thermostat"
                        ,multiple		: false
                        ,required		: true
                        ,type			: "capability.thermostat"
                        ,submitOnChange	: false
                    )
					input(
            			name			: "tempSensors"
                		,title			: "Thermostat temperature sensor:"
                		,multiple		: false
                		,required		: true
                		,type			: "capability.temperatureMeasurement"
                        ,submitOnChange	: false
            		) 
                    def froTitle = 'Delay zone cycle end notification is '
                    if (!fanRunOn || fanRunOn == "0"){
                    	froTitle = froTitle + "[off]"
                    } else {
                    	froTitle = froTitle + "[on]"
                    }
                    input(
            			name			: "fanRunOn"
                        ,title			: froTitle
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                		,options		: [["0":"Off"],["60":"1 Minute"],["120":"2 Minutes"],["180":"3 Minutes"],["240":"4 Minutes"],["300":"5 Minutes"]]
                        ,submitOnChange	: true
                   		,defaultValue	: "0"
            		)             
            }
            /*
            //if (state.etf){
            	//[["40":"4x10"],["48":"4x12"],["60":"6x10"],["72":"6x12"]]
            	section("Non Keen vent sizes"){
					input(
            			name			: "v20"
                		,title			: "Number of 2x10 vents"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                    	,submitOnChange	: false
                       	,options		: [["0":"0"],["20":"1"],["40":"2"],["60":"3"],["80":"4"],["100":"5"],["120":"6"],["140":"7"],["160":"8"],["180":"9"],["200":"10"]]
                        ,defaultValue	: "0"
            		) 
					input(
            			name			: "v40"
                		,title			: "Number of 4x10 vents"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                    	,submitOnChange	: false
                       	,options		: [["0":"0"],["40":"1"],["80":"2"],["120":"3"],["160":"4"],["200":"5"],["240":"6"],["280":"7"],["320":"8"],["360":"9"],["400":"10"]]
                        ,defaultValue	: "0"
            		) 
					input(
            			name			: "v48"
                		,title			: "Number of 4x12 vents"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                    	,submitOnChange	: false
                       	,options		: [["0":"0"],["48":"1"],["96":"2"],["144":"3"],["192":"4"],["240":"5"],["288":"6"],["336":"7"],["384":"8"],["432":"9"],["480":"10"]]
                        ,defaultValue	: "0"
            		) 
					input(
            			name			: "v56"
                		,title			: "Number of 4x14 vents"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                    	,submitOnChange	: false
                       	,options		: [["0":"0"],["56":"1"],["112":"2"],["168":"3"],["224":"4"],["280":"5"],["336":"6"],["392":"7"],["448":"8"],["504":"9"],["560":"10"]]
                        ,defaultValue	: "0"
            		) 
					input(
            			name			: "v60"
                		,title			: "Number of 6x10 vents"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                    	,submitOnChange	: false
                       	,options		: [["0":"0"],["60":"1"],["120":"2"],["180":"3"],["240":"4"],["300":"5"],["360":"6"],["420":"7"],["480":"8"],["540":"9"],["600":"10"]]
                        ,defaultValue	: "0"
            		) 
					input(
            			name			: "v72"
                		,title			: "Number of 6x12 vents"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                    	,submitOnChange	: false
                       	,options		: [["0":"0"],["72":"1"],["144":"2"],["216":"3"],["288":"4"],["360":"5"],["732":"6"],["504":"7"],["576":"8"],["648":"9"],["720":"10"]]
                        ,defaultValue	: "0"
            		) 

				}
            //} 
            */
            if (installed){
                section (getVersionInfo()) { }
            }
	}
}

def advanced(){
    return dynamicPage(
    	name		: "advanced"
        ,title		: "Advanced Options"
        ,install	: false
        ,uninstall	: false
        ){
         section(){
 			input(
            	name			: "setVo"
               	,title			: "Force vent opening to:"
               	,multiple		: false
               	,required		: true
               	,type			: "enum"
                ,options		:[["-1":"Do not change"],["0":"Fully closed"],["10":"10%"],["20":"20%"],["30":"30%"],["40":"40%"],["50":"50%"],["60":"60%"],["70":"70%"],["80":"80%"],["90":"90%"],["100":"Fully open"]]
                ,defaultValue	: "-1"
                ,submitOnChange	: true
            )     
            def vo = -1
            if (setVo){
            	vo = setVo.toInteger()
                if (vo > -1) paragraph (setChildVents(vo))
            }
         	input(
            	name			: "logLevel"
               	,title			: "IDE logging level" 
               	,multiple		: false
                ,required		: true
                ,type			: "enum"
 				,options		: getLogLevels()
                ,submitOnChange	: false
                ,defaultValue	: "10"
            )  
		    def etnTitle = sendEventsToNotifications ?  "Send lite events to notification feed is [on]" : "Send lite events to notification feed is [off]" 
          	input(
            	name			: "sendEventsToNotifications"
               	,title			: etnTitle 
               	,multiple		: false
               	,required		: false
               	,type			: "bool"
                ,submitOnChange	: true
                ,defaultValue	: false
            ) 
        }
    }
}

def reporting(){
	def report
	return dynamicPage(
    	name		: "reporting"
        ,title		: "Zone reports"
        ,install	: false
        ,uninstall	: false
        ){
    		section(){
            	report = "Configuration"
   				href( "report"
					,title		: report
					,description: ""
					,state		: null
					,params		: [rptName:report]
				) 
                report = "Current state"
                href( "report"
					,title		: report
					,description: ""
					,state		: null
					,params		: [rptName:report]
				)   
                report = "Last results"
                href( "report"
					,title		: report
					,description: ""
					,state		: null
					,params		: [rptName:report]
				)  
                /*
                if (state.etf){
                	report = "Percent open analysis"
                	href( "report"
						,title		: report
						,description: ""
						,state		: null
						,params		: [rptName:report]
					)  
                }
                */
            }
   }
}

def report(params){
	def reportName = params.rptName
	return dynamicPage(
    	name		: "report"
        ,title		: reportName
        ,install	: false
        ,uninstall	: false
        ){
    		section(){
   				paragraph(getReport(reportName))
            }
   }
}

def getReport(rptName){
	def cMethod
    def standardReport = false
    //[stat:[mainState:heat|cool|auto,mainMode:heat|cool|idle,mainCSP:,mainHSP:,mainOn:true|false]]
    def t = tempSensors.currentValue("temperature")
    def reports = ""
	if (rptName == "Current state"){
    	standardReport = true
    	cMethod = "getZoneState"
        //get whole house average temp
        def averageTemp = 0
        childApps.each{ child ->
        	averageTemp = averageTemp + child.getZoneTemp()
        }
        averageTemp = (averageTemp / childApps.size()).toDouble().round(1)
        reports = "Main system:\n\tstate: ${state.mainState}\n\tmode: ${state.mainMode}\n\tcurrent temp: ${tempStr(t)}\n\tcooling set point: ${tempStr(state.mainCSP)}\n\theating set point: ${tempStr(state.mainHSP)}\n\n"
        reports = reports + "Average zone temp: ${tempStr(averageTemp)}\n\n"
    } 
    if (rptName == "Configuration"){
    	standardReport = true
    	cMethod = "getZoneConfig"
        reports = "Main system:\n\tstate: ${state.mainState}\n\tmode: ${state.mainMode}\n\tcurrent temp: ${tempStr(t)}\n\tcooling set point: ${tempStr(state.mainCSP)}\n\theating set point: ${tempStr(state.mainHSP)}\n\n"
    }  
    if (rptName == "Last results"){
    	standardReport = true
    	cMethod = "getEndReport"
        def stime = "No data available yet"
        def etime = "No data available yet"
        def sTemp = tempStr(state.startTemp)
        def eTemp  = tempStr(state.endTemp)
        def rtm = "No data available yet"
        if ((state.startTime && state.endTime) && (state.startTime < state.endTime)){
        	stime = new Date(state.startTime).format("yyyy-MM-dd HH:mm")
            etime =  new Date(state.endTime).format("yyyy-MM-dd HH:mm")
            rtm = ((state.endTime - state.startTime) / 60000).toInteger()
            rtm = "${rtm} minutes"
        } 
        reports = "Main system:\n\tstart: ${stime}\n\tend: ${etime}\n\tstart temp: ${sTemp}\n\tend temp: ${eTemp}\n\tduration: ${rtm}\n\n"
    }
    if (standardReport){
    	def sorted = childApps.sort{it.label}
    	sorted.each{ child ->
    		//log.debug "getting child report for: ${child.label}"
       		try {
    			def report = child."${cMethod}"()
       			reports = reports + "Zone: " + child.label + "${report}" + "\n"
       		}
       		catch(e){}
        }
    } else {
    	//non standard reports
    	if (rptName == "Percent open analysis"){
    		cMethod = "getZoneSI"
        	def totalSI = v20.toInteger() + v40.toInteger() + v48.toInteger() + v56.toInteger() + v60.toInteger() + v72.toInteger()
        	def minSI = 0.0
        	def maxSI = 0.0
        	def crntSI = 0.0
        	childApps.each{ child ->
    			/*
            	[totalSI:totalSI,minSI:minSI,maxSI:maxSI,crntSI:crntSI]
				v56 is total non
            	*/
       			try {
    				def data = child."${cMethod}"()
                	totalSI = totalSI + data.totalSI
       				minSI = minSI + data.minSI
                	maxSI = maxSI + data.maxSI
                	crntSI = crntSI + data.crntSI
       			}
       			catch(e){}
    		}
            def pctMin = ((minSI / totalSI) * 100).toInteger()
            def pctMax = ((maxSI / totalSI) * 100).toInteger() 
            def pctCrnt = ((crntSI / totalSI) * 100).toInteger()
        	//actual report output
        	//reports = reports + "Zone: " + child.label + "${report}" + "\n"
            //reports = "totalSI: ${totalSI}\nminSI: ${minSI}\nmaxSI: ${maxSI}\ncrntSI: ${crntSI}\npctMin: ${pctMin}%\npctMax: ${pctMax}%\npctCrnt: ${pctCrnt}%\n"
            reports = "All vents at minimum\n\t${pctMin}%\nAll vents at maximum\n\t${pctMax}%\nCurrently\n\t${pctCrnt}%\n"
    	}
	}
    return reports
}

def checkNotify(evt){

	//logger(10|20|30|40,"error"|"warn"|"info"|"debug"|"trace",text)
    logger(40,"debug","checkNotify:enter- ")
	
	//log.debug "thermostat event- name: ${evt.name} value: ${evt.value} , description: ${evt.descriptionText}"
    //[stat:[mainMode:heat|cool|auto,mainState:heat|cool|idle,mainCSP:,mainHSP:,mainOn:true|false]]
	
    //[msg:"zone", data:[name:app.label,event:"installed"]]
    //def msg = params.msg
    //def initRequest = evt == "zoneRequest"
    //logger(30,"warn","checkNotify zoneRequest- from a new Zone")
	def tempStr = ''
    def tempFloat = 0.0
    def tempBool = false
    def isSetback = false
    def delay = settings.fanRunOn.toInteger()
	
    //thermostat state
	tempStr = getNormalizedOS(tStat.currentValue("thermostatOperatingState"))
	def mainState = state.mainState
    def mainStateChange = mainState != tempStr
    mainState = tempStr
    logger(40,"info","checkNotify- mainState: ${mainState}, mainStateChange: ${mainStateChange}")
    
    //thermostate mode
    tempStr = getNormalizedOS(tStat.currentValue("thermostatMode"))
    def mainMode = state.mainMode
    def mainModeChange = mainMode != tempStr
    mainMode = tempStr
    logger(40,"info","checkNotify- mainMode: ${mainMode}, mainModeChange: ${mainModeChange}")

	//cooling set point
	tempFloat = tStat.currentValue("coolingSetpoint").toFloat()
    def mainCSP = state.mainCSP
    def mainCSPChange = mainCSP != tempFloat
    //is setback? new csp > old csp
    isSetback = tempFloat > mainCSP
    mainCSP = tempFloat
    logger(40,"info","checkNotify- mainCSP: ${mainCSP}, mainCSPChange: ${mainCSPChange}")

	//heating set point
	tempFloat = tStat.currentValue("heatingSetpoint").toFloat()
    def mainHSP = state.mainHSP
    def mainHSPChange = mainHSP != tempFloat
    //is setback? new hsp < old hsp
    isSetback = tempFloat < mainHSP
    mainHSP = tempFloat
    logger(40,"info","checkNotify- mainHSP: ${mainHSP}, mainHSPChange: ${mainHSPChange}")
    
    def mainOn = mainState != "idle"
    
    /*
    //quick recovery
    def mainTemp = tempSensors.currentValue("temperature").toFloat()
    def mainQuick = state.mainQuick
    def mainQuickChange = false
    logger(30,"debug","checkNotify- quick recovery mainOn: ${mainOn}, isSetback: ${isSetback}")
    if (mainOn && !isSetback){
      	//mainTemp = tempFloat
        if (mainState == "heat"){
    		tempBool =  (mainTemp + 2) <= mainHSP
    	} else if (mainState == "cool") {
    		tempBool =  (mainTemp - 2) >= mainCSP
    	}
        mainQuickChange = tempBool != mainQuick
        mainQuick = tempBool
        logger(30,"debug","checkNotify- quick recovery active: ${mainQuick}, changed: ${mainQuickChange}")
    }
  	*/
    
    //always update state vars
    state.mainState = mainState
    state.mainMode = mainMode
    state.mainCSP = mainCSP
    state.mainHSP = mainHSP
    //state.mainQuick = mainQuick
    state.mainTemp = mainTemp
    
    //update cycle start data
    if (mainStateChange && mainOn){
    	//main start
        state.startTime = now() + location.timeZone.rawOffset
        state.startTemp = mainTemp
    } else if (mainStateChange && !mainOn){
    	//main end
        state.endTime = now() + location.timeZone.rawOffset
        state.endTemp = mainTemp
    }
    if (mainStateChange || mainModeChange || mainCSPChange || mainHSPChange){
    	def dataSet = [msg:"stat",data:[initRequest:false,mainState:mainState,mainStateChange:mainStateChange,mainMode:mainMode,mainModeChange:mainModeChange,mainCSP:mainCSP,mainCSPChange:mainCSPChange,mainHSP:mainHSP,mainHSPChange:mainHSPChange,mainOn:mainOn]]
        if (dataSet == state.dataSet){
        	//dup dataset..., should never ever happen
            logger(30,"warn","duplicate dataset, zones will not be notified... dataSet: ${state.dataSet}")
        } else {
        	logger(30,"debug","dataSet: ${dataSet}")
            if (mainStateChange) logger(10,"info","Main HVAC state changed to: ${mainState}")
        	if (mainModeChange) logger(10,"info","Main HVAC mode changed to: ${mainMode}")
        	if (mainCSPChange) logger(10,"info","Main HVAC cooling setpoint changed to: ${mainCSP}")
        	if (mainHSPChange) logger(10,"info","Main HVAC heating setpoint changed to: ${mainHSP}")
            state.dataSet = dataSet
            if (delay > 0){
				logger(10,"info", "Zone notification is scheduled in ${delaySeconds} delay")
				runIn(delay,notifyZones)
        	} else {
        		notifyZones()
        	}
        }
        //state.dataSet = [msg:"stat",data:[initRequest:false,mainState:mainState,mainStateChange:mainStateChange,mainMode:mainMode,mainModeChange:mainModeChange,mainCSP:mainCSP,mainCSPChange:mainCSPChange,mainHSP:mainHSP,mainHSPChange:mainHSPChange,mainOn:mainOn]]
    	//def dataSet = [msg:"stat",data:[initRequest:false,mainState:mainState,mainStateChange:mainStateChange,mainMode:mainMode,mainModeChange:mainModeChange,mainCSP:mainCSP,mainCSPChange:mainCSPChange,mainHSP:mainHSP,mainHSPChange:mainHSPChange,mainOn:mainOn,delay:delay,mainQuick:mainQuick,mainQuickChange:mainQuickChange]]
    }
    logger(40,"debug","checkNotify:exit- ")
}

def notifyZone(){
	//initial data request for new zone
    def mainState = getNormalizedOS(tStat.currentValue("thermostatOperatingState"))
    def mainMode = getNormalizedOS(tStat.currentValue("thermostatMode"))
    def mainCSP = tStat.currentValue("coolingSetpoint").toFloat()
    def mainHSP = tStat.currentValue("heatingSetpoint").toFloat()
    def mainOn = mainState != "idle"
	def dataSet = [msg:"stat",data:[initRequest:true,mainState:mainState,mainMode:mainMode,mainCSP:mainCSP,mainHSP:mainHSP,mainOn:mainOn]]
    logger(40,"debug","notifyZone:enter- map:${dataSet}")
    return dataSet
}

def notifyZones(){
    logger(40,"debug","notifyZones:enter- ")
    def dataSet = state.dataSet
    childApps.each {child ->
    	child.zoneEvaluate(dataSet)
    }
    logger(40,"debug","notifyZones:exit- ")
}

def setChildVents(vo){
	logger(40,"debug","setChildVents:enter- vo:${vo}")
    def result = "Setting zone vents to ${vo}%\n"
    childApps.each {child ->
    	child.setVents(vo)
        result = result + "\t${child.label}, was set...\n"
    }
    logger(40,"debug","setChildVents:exit- ")
    return result
}

def getNormalizedOS(os){
	def normOS = ""
    if (os == "heating" || os == "pending heat" || os == "heat" || os == "emergency heat"){
    	normOS = "heat"
    } else if (os == "cooling" || os == "pending cool" || os == "cool"){
    	normOS = "cool"
    } else if (os == "auto"){
    	normOS = "auto"
    } else if (os == "off"){
    	normOS = "off"
    } else {
    	normOS = "idle"
    }
    //log.debug "normOS- in:${os}, out:${normOS}"
    return normOS
}

def statHandler(evt){
	log.info "event:${evt.value}"

    def key = evt.date.format("yyyy-MM-dd HH:mm:ss")
    def v  = evt.value
    def evtTime = evt.date.getTime()
    if (v == "heating"){
        state.lastCalibrationStart = key
        state.startTime = evtTime
        state.startTemp = tempSensors.currentValue("temperature")
        log.info "start -time:${state.startTime} -temp:${state.startTemp}"
    	if (!state.lastDPH){
        	state.lastDPH = 0	
        } else {
        	state.crntDtemp = tStat.currentValue("heatingSetpoint") -  state.startTemp
            state.estDtime = state.crntDtemp / state.lastDPH
            
        }
    } else if (v == "idle" && state.startTime) {
    	//end
        state.endTime = evtTime
        def BigDecimal dTime = (state.endTime - state.startTime) / 3600000
        state.endTemp = tempSensors.currentValue("temperature")
        log.info "end -time:${state.endTime} -temp:${state.endTemp}"
        if (state.runTimes.size == 0){
        	state.runTimes = ["${key}":"runTime:${dTime} startTemp:${state.startTemp} endTemp:${state.endTemp}"]
        } else {
        	state.runTimes << ["${key}":"runTime:${dTime} startTemp:${state.startTemp} endTemp:${state.endTemp}"]
        }
        if (state.endTime > state.startTime && state.endTemp > state.startTemp ){
        	def BigDecimal dTemp  = (state.endTemp - state.startTemp)
            
            def BigDecimal dph = dTemp / dTime
            if (dTime >= 0.5) {
               	def value = ["CurrentDPH":"${dph}","lastDPH":"${state.lastDPH}" ,"ActualRunTime":"${dTime}","EstimatedRunTime":"${state.estDtime}" ,"ActualTempRise":"${dTemp}","EstimatedTempRise":"${state.crntDtemp}"]
        		log.info "${value}"
            	if (state.runMaps.size == 0){
            		state.runMaps = ["${key}":"${value}"]
            	} else {
            		state.runMaps << ["${key}":"${value}"]
            	}
            }
            state.lastDPH = dph
            state.endTime = ""
            state.startTime = ""
            state.endTemp = ""
            state.startTemp = ""
        }
        
    }
}

def getVersionInfo(){
	return "Versions:\n\tKeenect: ${state.vParent}\n\tkeenectZone: ${state.vChild ?: "No data available yet."}"
}

def updateVer(vChild){
    state.vChild = vChild
}

def tempStr(temp){
    def tc = state.tempScale ?: location.temperatureScale
    if (temp) return "${temp.toString()}°${tc}"
    else return "No data available yet."
}

def getSelectedDevices(deviceList){
	def deviceIDS = []
    deviceList.each{ device ->
    	deviceIDS.add(device.id)
    }
	return deviceIDS
}

def logger(displayLevel,errorLevel,text){
	//logger(10|20|30|40,"error"|"warn"|"info"|"debug"|"trace",text)
    /*
    [10:"Lite"],[20:"Moderate"],[30:"Detailed"],[40:"Super nerdy"]
 
    errorLevel 	color		number
    error		red			5
    warn		yellow		4
    info		lt blue		3
    debug		dk blue		2
    trace		gray		1
    */
    def logL = 0
    if (logLevel) logL = logLevel.toInteger()
    
    if (logL == 0) return //bail
    else if (logL >= displayLevel){
    	log."${errorLevel}"(text)
        if (sendEventsToNotifications && displayLevel == 10) {
          	def nixt = now() + location.timeZone.rawOffset
        	def today = new Date(nixt).format("HH:mm:ss.Ms")
        	text = "Main:" + today + ": " + text
        	sendNotificationEvent(text) //sendEvent(name: "kvParent", value: text, descriptionText: text, isStateChange : true)
        }
        //sendEvent(name: app.label , value: text, descriptionText: text, isStateChange : true)
    }
    //now()
    //def today = new Date() .format("yyyy-MM-dd HH:mm:ss")

}

def getLogLevel(val){
	def logLvl = 'Lite'
    def l = getLogLevels()
    if (val){
    	logLvl = l.find{ it."${val}"}
        logLvl = logLvl."${val}".value
    }
    return '[' + logLvl + ']'
}

def getLogLevels(){
	//return [["0":"None"],["10":"Lite"],["20":"Moderate"],["30":"Detailed"],["40":"Super nerdy"],["15":"Pressure only"]]
    return [["0":"None"],["10":"Lite"],["20":"Moderate"],["30":"Detailed"],["40":"Super nerdy"]]
}

def getID(){
	return state.etf
}

/*
	//spit out some time testing...
    def startTime = now() //epocMS, UTC
    def startTimeLocal = startTime + location.timeZone.rawOffset //epocMS, Local TZ
    def startTimeString = new Date(startTime).format("yyyy-MM-dd HH:mm")
    def startTimeStringLocal = new Date(startTimeLocal).format("yyyy-MM-dd HH:mm")
    log.info "times- startTime:${startTime} startTimeString:${startTimeString} startTimeLocal:${startTimeLocal} startTimeStringLocal:${startTimeStringLocal}"


*/