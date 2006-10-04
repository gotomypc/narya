//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2006 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.presents.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.samskivert.util.StringUtil;

/**
 * Primitively parses an ActionScriptSource file, allows the addition and
 * replacement of class members and handles writing out the file again.
 */
public class ActionScriptSource
{
    /**
     * Contains the name and text of a particular member declaration.
     */
    public static class Member
    {
        public String name;
        public String comment = "";
        public String definition;
        public String body = "";

        public Member (String name, String definition) {
            this.name = name;
            this.definition = definition;
        }

        public void setInitialValue (String initValue) {
            // do not-very-smart array conversion
            initValue = initValue.replaceAll("\\{", "[");
            initValue = initValue.replaceAll("\\}", "]");
            // stick the initial value before the semicolon
            definition = definition.substring(0, definition.length()-1) +
                " = " + initValue + ";";
        }

        public void setComment (String comment) {
            if (comment.indexOf("@Override") != -1) {
                comment = comment.replaceAll("@Override\\s?", "");
                definition = "override " + definition;
            }
            // trim blank lines from start
            while (comment.startsWith("\n")) {
                comment = comment.substring(1);
            }
            this.comment = comment;
        }

        public void write (PrintWriter writer) {
            writer.print(comment);
            writer.println("    " + definition);
            writer.print(body);
        }
    }

    public String preamble = "";

    public String packageName;

    public String imports = "";

    public String classComment = "";

    public boolean isAbstract;

    public String className;

    public String superClassName;

    public String[] implementedInterfaces;

    public ArrayList<Member> publicConstants = new ArrayList<Member>();

    public ArrayList<Member> publicFields = new ArrayList<Member>();

    public ArrayList<Member> publicStaticMethods = new ArrayList<Member>();

    public ArrayList<Member> publicConstructors = new ArrayList<Member>();

    public ArrayList<Member> publicMethods = new ArrayList<Member>();

    public ArrayList<Member> protectedConstructors = new ArrayList<Member>();

    public ArrayList<Member> protectedMethods = new ArrayList<Member>();

    public ArrayList<Member> protectedStaticMethods = new ArrayList<Member>();

    public ArrayList<Member> protectedFields = new ArrayList<Member>();

    public ArrayList<Member> protectedConstants = new ArrayList<Member>();

    public static String toSimpleName (String name)
    {
        name = name.substring(name.lastIndexOf(".")+1);
        // inner classes are not supported by ActionScript so we _
        name = name.replaceAll("\\$", "_");
        return name;
    }

    /**
     * Creates and returns a declaration for a field equivalent to the supplied
     * Java field in ActionScript.
     */
    public static String createActionScriptDeclaration (Field field)
    {
        int mods = field.getModifiers();
        StringBuilder builder = new StringBuilder();

        // what's our visibility?
        if (Modifier.isPublic(mods)) {
            builder.append("public ");
        } else if (Modifier.isProtected(mods)) {
            builder.append("protected ");
        } else if (Modifier.isPrivate(mods)) {
            builder.append("private ");
        }

        // are we static?
        if (Modifier.isStatic(mods)) {
            builder.append("static ");
        }

        // const or variable?
        builder.append(Modifier.isFinal(mods) ? "const " : "var ");

        // next comes the name
        builder.append(field.getName()).append(" :");

        // now convert the type to an ActionScript type
        builder.append(toActionScriptType(field.getType(),
                                          !Modifier.isStatic(mods)));

        builder.append(";");

        return builder.toString();
    }

    public static String createActionScriptDeclaration (
        Constructor ctor, boolean needsNoArg)
    {
        int mods = ctor.getModifiers();
        StringBuilder builder = new StringBuilder();

        // what's our visibility?
        if (Modifier.isPublic(mods)) {
            builder.append("public ");
        } else if (Modifier.isProtected(mods)) {
            builder.append("protected ");
        } else if (Modifier.isPrivate(mods)) {
            builder.append("private ");
        }

        // all constructors are functions
        builder.append("function ");

        // next comes the name
        builder.append(toSimpleName(ctor.getName())).append(" (");

        // now the parameters
        int idx = 0;
        for (Class<?> ptype : ctor.getParameterTypes()) {
            if (idx++ > 0) {
                builder.append(", ");
            }
            builder.append("arg").append(idx);
            builder.append(" :").append(toActionScriptType(ptype, false));
            if (needsNoArg) {
                builder.append(" = undef");
            }
        }
        builder.append(")");

        return builder.toString();
    }

    public static String createActionScriptDeclaration (Method method)
    {
        int mods = method.getModifiers();
        StringBuilder builder = new StringBuilder();

        // TODO: override

        // what's our visibility?
        if (Modifier.isPublic(mods)) {
            builder.append("public ");
        } else if (Modifier.isProtected(mods)) {
            builder.append("protected ");
        } else if (Modifier.isPrivate(mods)) {
            builder.append("private ");
        }

        // are we static const or variable?
        if (Modifier.isStatic(mods)) {
            builder.append("static ");
        }
        builder.append("function ");

        // next comes the name
        builder.append(getName(method)).append(" (");

        // now the parameters
        int idx = 0;
        for (Class<?> ptype : method.getParameterTypes()) {
            if (idx++ > 0) {
                builder.append(", ");
            }
            builder.append("arg").append(idx);
            builder.append(" :").append(toActionScriptType(ptype, false));
        }
        builder.append(") :");

        builder.append(toActionScriptType(method.getReturnType(), false));

        return builder.toString();
    }

    public static String toActionScriptType (Class type, boolean isField)
    {
        if (type.isArray()) {
            return isField ? "TypedArray" : "Array";
        }

        String tname = type.getName();
        if (tname.equals("java.lang.Integer") ||
            tname.equals("byte")) {
            return "int";
        }

        if (tname.equals("long")) {
            return "Long";
        }

        if (tname.equals("boolean")) {
            return "Boolean";
        }

        return toSimpleName(tname);
    }

    protected static String getName (Method method)
    {
        // yay for the hackery; we can't overload methods in ActionScript so we
        // do some crazy conversions based on signature to handle some of our
        // common patterns
        String name = method.getName();
        Class<?>[] ptypes = method.getParameterTypes();

        // toString(StringBuilder) -> toStringBuilder(StringBuilder)
        if (name.equals("toString") && ptypes.length == 1 &&
            ptypes[0].getName().equals("java.lang.StringBuilder")) {
            return "toStringBuilder";
        }

        return name;
    }

    public ActionScriptSource (Class jclass)
    {
        packageName = jclass.getPackage().getName();
        className = jclass.getName().substring(packageName.length()+1);
        Class sclass = jclass.getSuperclass();
        if (sclass != null && !sclass.getName().equals("java.lang.Object")) {
            superClassName = toSimpleName(sclass.getName());
        }
        isAbstract = ((jclass.getModifiers() & Modifier.ABSTRACT) != 0);

        // note our implemented interfaces
        ArrayList<String> ifaces = new ArrayList<String>();
        for (Class iclass : jclass.getInterfaces()) {
            // we cannot use the FooCodes interface pattern in ActionScript so
            // we just have to nix it
            if (iclass.getName().endsWith("Codes")) {
                continue;
            }
            ifaces.add(toSimpleName(iclass.getName()));
        }

        // convert the field members
        for (Field field : jclass.getDeclaredFields()) {
            int mods = field.getModifiers();
            ArrayList<Member> list;
            if (Modifier.isStatic(mods)) {
                list = Modifier.isPublic(mods) ?
                    publicConstants : protectedConstants;
            } else {
                list = Modifier.isPublic(mods) ? publicFields : protectedFields;
            }
            list.add(new Member(field.getName(),
                                createActionScriptDeclaration(field)));
        }

        // ActionScript only supports one constructor so we find the one with
        // the most arguments and we make a note whether or not we need a no
        // argument constructor which we create using default arguments
        Constructor mainctor = null;
        boolean needsNoArg = false;
        for (Constructor ctor : jclass.getConstructors()) {
            int params = ctor.getParameterTypes().length;
            if (mainctor == null ||
                params > mainctor.getParameterTypes().length) {
                mainctor = ctor;
            }
            needsNoArg = needsNoArg || (params == 0);
        }
        if (mainctor != null) {
            int mods = mainctor.getModifiers();
            ArrayList<Member> list;
            list = Modifier.isPublic(mods) ?
                publicConstructors : protectedConstructors;
            Member mem = new Member(
                toSimpleName(mainctor.getName()),
                createActionScriptDeclaration(mainctor, needsNoArg));
            mem.body = "    {\n        TODO: IMPLEMENT ME\n    }\n";
            list.add(mem);
        }

        for (Method method : jclass.getDeclaredMethods()) {
            int mods = method.getModifiers();
            ArrayList<Member> list;
            if (Modifier.isStatic(mods)) {
                list = Modifier.isPublic(mods) ?
                    publicStaticMethods : protectedStaticMethods;
            } else {
                list = Modifier.isPublic(mods) ?
                    publicMethods : protectedMethods;
            }

            // see if we already have a member for this method name
            String name = getName(method);
            String decl = createActionScriptDeclaration(method);
            Member mem = getMember(list, name);
            if (mem == null) {
                mem = new Member(name, decl);
                mem.body = "    {\n        TODO: IMPLEMENT ME\n    }\n";
                list.add(mem);
            } else {
                // TODO: only overwrite if we have more arguments
                mem.definition = decl;
            }
        }

        // if we override hashCode() then add Hashable to our interfaces
        if (getMember(publicMethods, "hashCode") != null) {
            ifaces.add("Hashable");
        }

        // finally turn our implemented interfaces into an array
        implementedInterfaces = ifaces.toArray(new String[ifaces.size()]);
    }

    public void absorbJava (File jsource)
        throws IOException
    {
        // parse our Java source file for niggling bits
        BufferedReader bin = new BufferedReader(new FileReader(jsource));
        String line = null;
        Mode mode = Mode.PREAMBLE;
        Matcher m;
        int braceCount = 0;
        StringBuilder accum = new StringBuilder();
        while ((line = bin.readLine()) != null) {
            line = line.replaceAll("\\s+$", "");
            switch (mode) {
            case PREAMBLE:
                // look for the package declaration
                m = JPACKAGE.matcher(line);
                if (m.matches()) {
                    preamble = accum.toString();
                    accum.setLength(0);
                    packageName = m.group(1);
                    mode = Mode.IMPORTS;
                } else {
                    accum.append(line).append("\n");
                }
                break;

            case IMPORTS:
                // look for the start of the class comment
                if (line.startsWith("/**")) {
                    imports = accum.toString();
                    accum.setLength(0);
                    mode = Mode.CLASSCOMMENT;
                }
                accum.append(line).append("\n");
                break;

            case CLASSCOMMENT:
                // look for the class declaration
                if (line.startsWith("public ")) {
                    classComment = accum.toString();
                    accum.setLength(0);
                    mode = line.endsWith("{") ? Mode.CLASSBODY : Mode.CLASSDECL;
                } else {
                    accum.append(line).append("\n");
                }
                break;

            case CLASSDECL:
                // look for the open brace
                if (line.equals("{")) {
                    mode = Mode.CLASSBODY;
                }
                break;

            case CLASSBODY:
                // see if we match a field declaration
                if ((m = JFIELD.matcher(line)).matches()) {
                    // if this line does not end on a semicolon, keep sucking
                    // up lines until it does
                    if (line.indexOf(";") == -1) {
                        line = line + slurpUntil(bin, ";", true);
                        // now rematch it all on one line
                        m = JFIELD.matcher(line);
                        if (!m.matches()) {
                            System.err.println(
                                "J: Pants, no longer match field: " + line);
                            continue;
                        }
                    }

                    String name = m.group(1);
                    String comment = accum.toString();
                    accum.setLength(0);

                    // set the comment on the specified field
                    Member mem = updateComment(publicConstants, name, comment);
                    if (mem == null) {
                        mem = updateComment(publicFields, name, comment);
                    }
                    if (mem == null) {
                        mem = updateComment(protectedFields, name, comment);
                    }
                    if (mem == null) {
                        mem = updateComment(protectedConstants, name, comment);
                    }
                    if (mem == null) {
                        System.err.println("J: Matched field for which we " +
                                           "have no bytecode version: " + name +
                                           ": " + line);
                        continue;
                    }

                    // extract and clean up any default value
                    String defval = m.group(2).trim();
                    if (defval.startsWith("=")) {
                        defval = defval.substring(1);
                    }
                    if (defval.endsWith(";")) {
                        defval = defval.substring(0, defval.length()-1);
                    }
                    defval = defval.trim();
                    if (defval.length() > 0) {
                        mem.setInitialValue(defval);
                    }

                // see if we match a constructor declaration
                } else if ((m = JCONSTRUCTOR.matcher(line)).matches()) {
                    // if this line does not contain a close paren, keep
                    // sucking up lines until it does
                    if (line.indexOf(")") == -1) {
                        line = line + slurpUntil(bin, ")", true);
                        // now rematch it all on one line
                        m = JCONSTRUCTOR.matcher(line);
                        if (!m.matches()) {
                            System.err.println(
                                "J: Pants, no longer match ctor: " + line);
                            continue;
                        }
                    }

                    String name = m.group(1);
                    String comment = accum.toString();
                    accum.setLength(0);
                    // set the comment on the specified method
                    updateComment(publicConstructors, name, comment);
                    updateComment(protectedConstructors, name, comment);

                    // switch to METHODBODY to absorb the method
                    braceCount = (line.indexOf("{") == -1) ? 0 : 1;
                    mode = Mode.METHODBODY;

                // see if we match a method declaration
                } else if ((m = JMETHOD.matcher(line)).matches()) {
                    // if this line does not contain a close paren, keep
                    // sucking up lines until it does
                    if (line.indexOf(")") == -1) {
                        line = line + slurpUntil(bin, ")", true);
                        // now rematch it all on one line
                        m = JMETHOD.matcher(line);
                        if (!m.matches()) {
                            System.err.println(
                                "J: Pants, no longer match method: " + line);
                            continue;
                        }
                    }

                    String name = m.group(1);
                    String comment = accum.toString();
                    accum.setLength(0);
                    // set the comment on the specified method
                    updateComment(publicStaticMethods, name, comment);
                    updateComment(publicMethods, name, comment);
                    updateComment(protectedMethods, name, comment);
                    updateComment(protectedStaticMethods, name, comment);

                    // switch to METHODBODY to absorb the method
                    braceCount = (line.indexOf("{") == -1) ? 0 : 1;
                    mode = Mode.METHODBODY;

                } else {
                    // make sure this is a comment (or annotation)
                    String tline = line.trim();
                    if (tline.length() == 0 || tline.startsWith("@") ||
                        tline.startsWith("//") || tline.startsWith("/*") ||
                        tline.startsWith("*")) {
                        accum.append(line).append("\n");

                    } else if (tline.equals("}")) {
                        mode = Mode.POSTCLASS;

                    } else {
                        System.err.println("J: Non-comment encountered " +
                                           "between members: " + line);
                    }
                }
                break;

            case METHODBODY:
                if (line.indexOf("{") != -1) {
                    braceCount++;
                }
                if (line.indexOf("}") != -1) {
                    braceCount--;
                }
                if (braceCount == 0) {
                    mode = Mode.CLASSBODY;
                }
                break;

            case POSTCLASS:
                System.err.println("J: Post-class junk: " + line);
                break;
            }
        }
        bin.close();

        if (accum.length() > 0) {
            System.err.println("J: Leftover accumulated text: " + accum);
        }
    }

    public void absorbActionScript (File assource)
        throws IOException
    {
        // if our action script source file doesn't exist, we're done
        if (!assource.exists()) {
            return;
        }

        BufferedReader bin = new BufferedReader(new FileReader(assource));
        String line = null;
        Mode mode = Mode.PREAMBLE;
        Matcher m;
        int braceCount = 0;
        StringBuilder accum = new StringBuilder();
        Member curmem = null;
        while ((line = bin.readLine()) != null) {
            line = line.replaceAll("\\s+$", "");
            switch (mode) {
            case PREAMBLE:
                // look for the package declaration
                if (line.startsWith("package ")) {
                    preamble = accum.toString();
                    accum.setLength(0);
                    mode = Mode.IMPORTS;
                } else {
                    accum.append(line).append("\n");
                }
                break;

            case IMPORTS:
                // look for the start of the class comment
                if (line.startsWith("/**")) {
                    imports = accum.toString();
                    accum.setLength(0);
                    mode = Mode.CLASSCOMMENT;
                }
                accum.append(line).append("\n");
                break;

            case CLASSCOMMENT:
                // look for the class declaration
                if (line.startsWith("public ")) {
                    classComment = accum.toString();
                    accum.setLength(0);
                    mode = line.endsWith("{") ? Mode.CLASSBODY : Mode.CLASSDECL;
                } else {
                    accum.append(line).append("\n");
                }
                break;

            case CLASSDECL:
                // look for the open brace
                if (line.equals("{")) {
                    mode = Mode.CLASSBODY;
                }
                break;

            case CLASSBODY:
                // see if we match a field declaration
                if ((m = ASFIELD.matcher(line)).matches()) {
                    // if this line does not contain a semicolon, keep sucking
                    // up lines until it does
                    if (line.indexOf(";") == -1) {
                        line = line + "\n" + slurpUntil(bin, ";", false);
                    }

                    String fieldName = m.group(1);

                    // TODO: update the comment?
                    accum.setLength(0);

                    // TODO: extract the name, replace that declaration

                // see if we match a constructor declaration
                } else if (line.indexOf("public function " + className) != -1) {
                    // if this line does not contain a close paren, keep
                    // sucking up lines until it does
                    if (line.indexOf(")") == -1) {
                        line = line + "\n" + slurpUntil(bin, ")", false);
                    }

                    // look up the public constructor
                    curmem = getMember(publicConstructors, className);
                    if (curmem == null) {
                        System.err.println("Missing public constructor for " +
                                           "update " + className + "().");
                    } else {
                        // update the definition and comment with the
                        // ActionScript versions
                        curmem.definition = line.trim();
                        curmem.setComment(accum.toString());
                    }
                    accum.setLength(0);

                    // switch to METHODBODY to absorb the method
                    braceCount = (line.indexOf("{") == -1) ? 0 : 1;
                    mode = Mode.METHODBODY;

                // see if we match a method declaration
                } else if ((m = ASFUNCTION.matcher(line)).matches()) {
                    // if this line does not contain a close paren, keep
                    // sucking up lines until it does
                    if (line.indexOf(")") == -1) {
                        line = line + "\n" + slurpUntil(bin, ")", false);
                    }

                    // extract the name, replace the declaration
                    String funcName = m.group(1);
                    curmem = getMember(publicMethods, funcName);
                    if (curmem == null) {
                        curmem = getMember(protectedMethods, funcName);
                    }
                    if (curmem == null) {
                        System.err.println(
                            "Have ActionScript method with no " +
                            "Java equivalent: " + funcName + "().");
                    } else {
                        // update the definition and comment with the
                        // ActionScript versions (unless it's readObject or
                        // writeObject in which case we want always to use the
                        // generated versions)
                        if (!funcName.equals("readObject") &&
                            !funcName.equals("writeObject")) {
                            curmem.definition = line.trim();
                        }
                        curmem.setComment(accum.toString());
                    }
                    accum.setLength(0);

                    // switch to METHODBODY to absorb the method
                    braceCount = (line.indexOf("{") == -1) ? 0 : 1;
                    mode = Mode.METHODBODY;

                } else {
                    // make sure this is a comment (or annotation)
                    String tline = line.trim();
                    if (tline.length() == 0 || tline.startsWith("@") ||
                        tline.startsWith("//") || tline.startsWith("/*") ||
                        tline.startsWith("*")) {
                        accum.append(line).append("\n");

                    } else if (tline.equals("}")) {
                        mode = Mode.POSTCLASS;

                    } else {
                        System.err.println("AS: Non-comment encountered " +
                                           "between members: " + line);
                    }
                }
                break;

            case METHODBODY:
                if (line.indexOf("{") != -1) {
                    braceCount++;
                }
                if (line.indexOf("}") != -1) {
                    braceCount--;
                }
                accum.append(line).append("\n");
                if (braceCount == 0) {
                    // update the method body of our currently matched member
                    if (curmem != null) {
                        curmem.body = accum.toString();
                        curmem = null;
                    } else {
                        System.err.println(
                            "AS: No matched method for body:\n" + accum);
                    }
                    accum.setLength(0);
                    mode = Mode.CLASSBODY;
                }
                break;

            case POSTCLASS:
                if (line.trim().equals("}")) {
                    mode = Mode.POSTPKG;
                } else {
                    System.err.println("AS: Post-class junk: " + line);
                }
                break;

            case POSTPKG:
                System.err.println("AS: Post-package junk: " + line);
                break;
            }
        }
        bin.close();

        if (accum.length() > 0) {
            System.err.println("AS: Leftover accumulated text: " + accum);
        }
    }

    public Member updateComment (
        ArrayList<Member> list, String name, String comment)
    {
        Member mem = getMember(list, name);
        if (mem != null) {
            mem.setComment(comment);
        }
        return mem;
    }

    public Member getMember (ArrayList<Member> list, String name)
    {
        for (Member member : list) {
            if (member.name.equals(name)) {
                return member;
            }
        }
        return null;
    }

    /**
     * Writes our class definition to the supplied writer.
     */
    public void write (PrintWriter writer)
    {
        writer.print(preamble);
        writer.println("package " + packageName + " {");
        writer.print(imports);

        writer.print(classComment);
        writer.print("public ");
        if (isAbstract) {
            writer.print("/*abstract*/ ");
        }
        writer.print("class " + className);
        if (superClassName != null) {
            writer.print(" extends " + superClassName);
        }
        writer.println("");

        if (implementedInterfaces.length > 0) {
            writer.print("    implements ");
            for (int ii = 0; ii < implementedInterfaces.length; ii++) {
                if (ii > 0) {
                    writer.print(", ");
                }
                writer.print(implementedInterfaces[ii]);
            }
            writer.println("");
        }

        writer.println("{"); // start class block

        boolean writtenAnything = false;
        for (Member member : publicConstants) {
            writtenAnything = writeBlank(writtenAnything, writer);
            member.write(writer);
        }

        for (Member member : publicFields) {
            writtenAnything = writeBlank(writtenAnything, writer);
            member.write(writer);
        }

        for (Member member : publicStaticMethods) {
            writtenAnything = writeBlank(writtenAnything, writer);
            member.write(writer);
        }

        for (Member member : publicConstructors) {
            writtenAnything = writeBlank(writtenAnything, writer);
            member.write(writer);
        }

        for (Member member : publicMethods) {
            writtenAnything = writeBlank(writtenAnything, writer);
            member.write(writer);
        }

        for (Member member : protectedConstructors) {
            writtenAnything = writeBlank(writtenAnything, writer);
            member.write(writer);
        }

        for (Member member : protectedMethods) {
            writtenAnything = writeBlank(writtenAnything, writer);
            member.write(writer);
        }

        for (Member member : protectedStaticMethods) {
            writtenAnything = writeBlank(writtenAnything, writer);
            member.write(writer);
        }

        for (Member member : protectedFields) {
            writtenAnything = writeBlank(writtenAnything, writer);
            member.write(writer);
        }

        for (Member member : protectedConstants) {
            writtenAnything = writeBlank(writtenAnything, writer);
            member.write(writer);
        }

        writer.println("}"); // end class block
        writer.println("}"); // end package block
        writer.flush();
    }

    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("public ");
        if (isAbstract) {
            builder.append("abstract ");
        }
        builder.append("class ").append(className);
        if (superClassName != null) {
            builder.append(" extends ").append(superClassName);
        }
        return builder.toString();
    }

    protected String slurpUntil (BufferedReader reader, String token,
                                 boolean stripNewlines)
        throws IOException
    {
        String text = "", line;
        while ((line = reader.readLine()) != null) {
            line = line.replaceAll("\\s+$", "");
            text += line;
            if (!stripNewlines) {
                text += "\n";
            }
            if (line.indexOf(token) != -1) {
                break;
            }
        }
        return text;
    }

    protected boolean writeBlank (boolean writtenAnything, PrintWriter writer)
    {
        if (writtenAnything) {
            writer.println("");
        }
        return true;
    }

    protected static enum Mode { PREAMBLE, IMPORTS, CLASSCOMMENT, CLASSDECL,
                                 CLASSBODY, METHODBODY, POSTCLASS, POSTPKG };

    protected static Pattern JPACKAGE = Pattern.compile("package\\s+(\\S+);");

    protected static Pattern JFIELD = Pattern.compile(
        "\\s+(?:public|protected|private)" +
        "(?:\\s+static|\\s+final|\\s+transient)*" +
        "\\s+(?:[a-zA-Z\\[\\]<>,]+)" + // type
        "\\s+([_a-zA-Z]\\w*)(\\s+=.*|;)");

    protected static Pattern JCONSTRUCTOR = Pattern.compile(
        "\\s+(?:public|protected|private)\\s+([a-zA-Z]\\w*) \\(.*");

    protected static Pattern JMETHOD = Pattern.compile(
        "\\s+(?:public|protected|private).* ([a-zA-Z]\\w*) \\(.*");

    protected static Pattern ASFIELD = Pattern.compile(
        "\\s+(?:public|protected|private)" +
        "(?:\\s+var|\\s+static\\s+const)?" +
        "\\s+([_a-zA-Z]\\w*)" + // variable name
        "\\s+(?::[a-zA-Z\\[\\]<>,]+)" + // type
        "(\\s+=.*|;)");

    protected static Pattern ASFUNCTION = Pattern.compile(
        ".*(?:public|protected) function ([a-zA-Z]\\w*) \\(.*");
}
