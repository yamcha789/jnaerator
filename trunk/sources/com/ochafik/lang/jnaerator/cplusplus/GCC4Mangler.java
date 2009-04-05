/*
	Copyright (c) 2009 Olivier Chafik, All Rights Reserved
	
	This file is part of JNAerator (http://jnaerator.googlecode.com/).
	
	JNAerator is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	JNAerator is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.
	
	You should have received a copy of the GNU Lesser General Public License
	along with JNAerator.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.ochafik.lang.jnaerator.cplusplus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ochafik.lang.jnaerator.Result;
import com.ochafik.lang.jnaerator.parser.Arg;
import com.ochafik.lang.jnaerator.parser.Element;
import com.ochafik.lang.jnaerator.parser.Function;
import com.ochafik.lang.jnaerator.parser.Modifier;
import com.ochafik.lang.jnaerator.parser.Struct;
import com.ochafik.lang.jnaerator.parser.TypeRef;
import com.ochafik.lang.jnaerator.parser.Declarator.PointerStyle;
import com.ochafik.lang.jnaerator.parser.TypeRef.FunctionSignature;
import com.ochafik.lang.jnaerator.parser.TypeRef.SimpleTypeRef;
import com.ochafik.lang.jnaerator.parser.TypeRef.TaggedTypeRef;

public class GCC4Mangler implements CPlusPlusMangler {
	
	protected void mangleType(TypeRef tr, StringBuilder b, Result result) {
		if (tr instanceof TypeRef.TargettedTypeRef) {
			if (tr instanceof TypeRef.Pointer && ((TypeRef.Pointer)tr).getPointerStyle() == PointerStyle.Reference)
				b.append("R");
			else
				b.append("P");
			if (Modifier.Const.isContainedBy(tr.getModifiers()))
				b.append("K");
			mangleType(((TypeRef.TargettedTypeRef)tr).getTarget(), b, result);
		} else if (tr instanceof TypeRef.SimpleTypeRef) {
			SimpleTypeRef str = (SimpleTypeRef) tr;
			TypeRef resolved = result.typeConverter.resolveTypeDef(str, null, false);
			if (resolved != null && !resolved.toString().equals(str.toString())) {
				mangleType(resolved, b, result);
				return;
			}
			Primitive p = Primitive.parsePrimitive(str);
			String s = signatures.get(p);
			if (s != null)
				b.append(s);
			else {
				lenghtedName(str.getName(), b);
			}
		} else if (tr instanceof TaggedTypeRef) {
			lenghtedName(((TaggedTypeRef)tr).getOriginalTag(), b);
		} else if (tr instanceof FunctionSignature) {
			FunctionSignature fs = (FunctionSignature) tr;
			b.append("PF");
			mangleType(fs.getFunction().getValueType(), b, result);
			mangleArgs(fs.getFunction().getArgs(), b, result);
			b.append("E");
		} else {
			throw new UnsupportedOperationException("Cannot mangle type references of class " + tr.getClass().getName() + " : '" + tr + "'");
		}
	}
	protected void lenghtedName(String name, StringBuilder out) {
		out.append(name.length() + name);
	}
	public String mangle(Function function, Result result) {
		String name = function.getName();
		if (name == null)
			return null;
		
		StringBuilder b = new StringBuilder("_Z");
		
		Element parent = function.getParentElement();
		List<String> ns;
		boolean isMethod = parent instanceof Struct;
		if (isMethod) {
			ns = new ArrayList<String>(parent.getNameSpace());
			ns.add(((Struct)parent).getTag());
		} else {
			ns = new ArrayList<String>(function.getNameSpace());
		}
		
		if (ns.isEmpty()) {
			lenghtedName(name, b);
		} else {
			b.append("N");
			for (String n : ns)
				lenghtedName(n, b);
			lenghtedName(name, b);
			if (isMethod)
				b.append("E");
		}
		
		mangleArgs(function.getArgs(), b, result);
		return b.toString();
	}
	private void mangleArgs(List<Arg> args, StringBuilder b, Result result) {

		if (args.isEmpty())
			b.append("v");
		else {
			for (Arg arg : args) {
				TypeRef tr = arg.createMutatedType();
				mangleType(tr, b, result);
			}
		}
	}
	static Map<Primitive, String> signatures = new HashMap<Primitive, String>();
	static {
		signatures.put(Primitive.Void, 		"v");
		signatures.put(Primitive.Char, 		"c"); 
		signatures.put(Primitive.SChar, 	"c"); 
		signatures.put(Primitive.UChar,	 	"h"); 
		signatures.put(Primitive.Long, 		"l"); 
		signatures.put(Primitive.ULong,	 	"m"); 
		signatures.put(Primitive.Int, 		"i"); 
		signatures.put(Primitive.UInt, 		"j"); 
		signatures.put(Primitive.Short,	 	"s"); 
		signatures.put(Primitive.UShort, 	"t"); 
		signatures.put(Primitive.Bool, 		"b"); 
		signatures.put(Primitive.Float, 	"f"); 
		signatures.put(Primitive.Double, 	"d");
	}
}
