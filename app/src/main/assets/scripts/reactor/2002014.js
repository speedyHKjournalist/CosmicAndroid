/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*@author Ronan
 *Reactor : OrbisPQ Bonus Reactor - 2002014.js
 * Drops all the Bonus Items
 */

function act() {
    rm.dropItems(true, 1, 100, 400, 15);

    var eim = rm.getEventInstance();
    if (eim.getProperty("statusStgBonus") != "1") {
        rm.spawnNpc(2013002, new android.graphics.Point(46, 840));
        eim.setProperty("statusStgBonus", "1");
    }
}