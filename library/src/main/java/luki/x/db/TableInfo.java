/**
 * Copyright (C) 2014 Luki(liulongke@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package luki.x.db;

import java.io.Serializable;

import luki.x.db.annotation.Unique;
import luki.x.db.annotation.TableVersion;

/**
 * DB table info.
 * 
 * @author Luki
 * @date Aug 22, 2014 3:23:44 PM
 */
@TableVersion(1)
public class TableInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5121696252854772406L;

	@Unique
	public String tableName;
	public String tableClass;
	public int tableVersion = 1;

}
