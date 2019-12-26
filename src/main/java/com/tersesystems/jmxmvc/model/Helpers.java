/**
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright ${year} ${name} <${email}>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tersesystems.jmxmvc.model;

import javax.management.ObjectName;
import java.util.Map;

public class Helpers {

    public static boolean matches(ObjectName name,
                            ObjectName pattern) {
        if (pattern == null) return true;
        final String od = name.getDomain();
        if (!od.equals("")) {
            final String domain = pattern.getDomain();
            if (!wildmatch(od.toCharArray(), domain.toCharArray(), 0, 0))
                return false;
        }
        if (pattern.isPropertyPattern()) {
            for (Map.Entry<String, String> e : pattern.getKeyPropertyList().entrySet()) {
                final String key = e.getKey();
                final String value = e.getValue();
                final String v = name.getKeyProperty(key);
                if (v == null && value != null) return false;
                if (v.equals(value)) continue;
                return false;
            }
            return true;
        } else {
            final String p1 = name.getCanonicalKeyPropertyListString();
            final String p2 = pattern.getCanonicalKeyPropertyListString();
            if (p1 == null) return (p2 == null);
            if (p2 == null) return p1.equals("");
            return (p1.equals(p2));
        }
    }

    public static boolean wildmatch(char[] s, char[] p, int si, int pi) {
        char c;
        final int slen = s.length;
        final int plen = p.length;

        while (pi < plen) {            // While still string
            c = p[pi++];
            if (c == '?') {
                if (++si > slen) return false;
            } else if (c == '*') {        // Wildcard
                if (pi >= plen) return true;
                do {
                    if (wildmatch(s, p, si, pi)) return true;
                } while (++si < slen);
                return false;
            } else {
                if (si >= slen || c != s[si++]) return false;
            }
        }
        return (si == slen);
    }

}
