/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oodt.cas.cl.option.handler;

//OODT imports
import org.apache.oodt.cas.cl.action.CmdLineAction;
import org.apache.oodt.cas.cl.option.CmdLineOption;
import org.apache.oodt.cas.cl.option.CmdLineOptionInstance;

/**
 * Handles a {@link CmdLineOption}'s values in relation to given
 * {@link CmdLineAction}s. 
 *
 * @author bfoster (Brian Foster)
 */
public interface CmdLineOptionHandler {

	public abstract void handleOption(CmdLineAction selectedAction, CmdLineOptionInstance optionInstance);

	/**
	 * Gets the {@link CmdLineOptionHandler}s help message when associated with given {@link CmdLineOption}.
	 *
	 * @param option The {@link CmdLineOption} to which this {@link CmdLineOptionHandler} was associated with
	 * @return The help message for this {@link CmdLineOptionHandler}
	 */
	public abstract String getHelp(CmdLineOption option);

}
