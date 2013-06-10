/*
 * Copyright (C) 2013 OSRF.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.ros_java.android_apps.android_app_chooser.zeroconf;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import com.github.ros_java.zeroconf_jmdns_suite.jmdns.DiscoveredService;
import com.github.ros_java.zeroconf_jmdns_suite.jmdns.ZeroconfDiscoveryHandler;

/**
 * This class is the callback handler for services being listened for
 * by the jmdns zeroconf class. 
 * 
 * Usually we should do a bit of checking to make sure that any 
 * service isn't getting repeated on another interface, but for
 * now we can assume your android has only the one interface so that
 * we handle each added/resolved/removed as a unique entry.
 */
public class DiscoveryHandler implements ZeroconfDiscoveryHandler {

	/*********************
	 * Tasks
	 ********************/
	@SuppressLint("NewApi")
	private class ServiceAddedTask extends AsyncTask<DiscoveredService, String, Void> {
		
	    @SuppressLint("NewApi")
		protected Void doInBackground(DiscoveredService... services) {
	        if ( services.length == 1 ) {
                DiscoveredService service = services[0];
				String result = "[+] Service added: " + service.name + "." + service.type + "." + service.domain + ".";
				publishProgress(result);
	        } else {
	            publishProgress("Error - ServiceAddedTask::doInBackground received #services != 1");
	        }
	        return null;
	    }

	}

	@SuppressLint("NewApi")
	private class ServiceResolvedTask extends AsyncTask<DiscoveredService, String, DiscoveredService> {
		
	    @SuppressLint("NewApi")
		protected DiscoveredService doInBackground(DiscoveredService... services) {
	        if ( services.length == 1 ) {
                DiscoveredService discovered_service = services[0];
		    	String result = "[=] Service resolved: " + discovered_service.name + "." + discovered_service.type + "." + discovered_service.domain + ".\n";
		    	result += "    Port: " + discovered_service.port;
                for ( String address : discovered_service.ipv4_addresses ) {
                    result += "\n    Address: " + address;
                }
                for ( String address : discovered_service.ipv6_addresses ) {
                    result += "\n    Address: " + address;
                }
		    	publishProgress(result);
		    	return discovered_service;
	        } else {
	            publishProgress("Error - ServiceAddedTask::doInBackground received #services != 1");
	        }
	        return null;
	    }
	    
	    @SuppressLint("NewApi")
		protected void onPostExecute(DiscoveredService discovered_service) {
	    	// add to the content and notify the list view if its a new service
	    	if ( discovered_service != null ) {
				int index = 0;
				for ( DiscoveredService s : discovered_services ) {
					if ( s.name.equals(discovered_service.name) ) {
						break;
					} else {
						++index;
					}
				}
				if ( index == discovered_services.size() ) {
					discovered_services.add(discovered_service);
					discovery_adapter.notifyDataSetChanged();
				} else {
					android.util.Log.i("zeroconf", "Tried to add an existing service (fix this)");
				}
	    	}
	    }
	}
	
	@SuppressLint("NewApi")
	private class ServiceRemovedTask extends AsyncTask<DiscoveredService, String, DiscoveredService> {
		
	    @SuppressLint("NewApi")
		protected DiscoveredService doInBackground(DiscoveredService... services) {
	        if ( services.length == 1 ) {
                DiscoveredService discovered_service = services[0];
	            String result = "[-] Service removed: " + discovered_service.name + "." + discovered_service.type + "." + discovered_service.domain + ".\n";
	            result += "    Port: " + discovered_service.port;
		    	publishProgress(result);
		    	return discovered_service;
	        } else {
	            publishProgress("Error - ServiceAddedTask::doInBackground received #services != 1");
	        }
	        return null;
	    }

	    
	    protected void onPostExecute(DiscoveredService discovered_service) {
	    	// remove service from storage and notify list view
	    	if ( discovered_service != null ) {
				int index = 0;
				for ( DiscoveredService s : discovered_services ) {
					if ( s.name.equals(discovered_service.name) ) {
						break;
					} else {
						++index;
					}
				}
				if ( index != discovered_services.size() ) {
					discovered_services.remove(index);
					discovery_adapter.notifyDataSetChanged();
				} else {
					android.util.Log.i("zeroconf", "Tried to remove a non-existant service");
				}
	    	}
	    }
	}

	/*********************
	 * Variables
	 ********************/
	private ArrayList<DiscoveredService> discovered_services;
    private DiscoveryAdapter discovery_adapter;

	/*********************
	 * Constructors
	 ********************/
	public DiscoveryHandler(DiscoveryAdapter discovery_adapter, ArrayList<DiscoveredService> discovered_services) {
		this.discovery_adapter = discovery_adapter;
		this.discovered_services = discovered_services;
	}

	/*********************
	 * Callbacks
	 ********************/
	public void serviceAdded(DiscoveredService service) {
		new ServiceAddedTask().execute(service);
	}
	
	public void serviceRemoved(DiscoveredService service) {
		new ServiceRemovedTask().execute(service);
	}
	
	public void serviceResolved(DiscoveredService service) {
		new ServiceResolvedTask().execute(service);
	}
}
