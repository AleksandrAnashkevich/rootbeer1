/* 
 * Copyright 2012 Phil Pratt-Szeliga and other contributors
 * http://chirrup.org/
 * 
 * See the file LICENSE for copying permission.
 */

package edu.syr.pcpratts.rootbeer.entry;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import soot.SootField;
import soot.Type;
import soot.rbclassload.ClassHierarchy;
import soot.rbclassload.DfsInfo;
import soot.rbclassload.FieldSignatureUtil;
import soot.rbclassload.HierarchySignature;
import soot.rbclassload.HierarchyValueSwitch;
import soot.rbclassload.MethodSignatureUtil;
import soot.rbclassload.RootbeerClassLoader;
import soot.rbclassload.StringNumbers;
import soot.rbclassload.StringToType;

/**
 *
 * @author pcpratts
 */
public class RootbeerDfs {
  
  private DfsInfo m_currDfsInfo;
  
  public void run(DfsInfo dfs_info) {
    m_currDfsInfo = dfs_info;
    String signature = dfs_info.getRootMethodSignature();
    
    Set<HierarchySignature> visited = new HashSet<HierarchySignature>();
    //System.out.println("doing rootbeer dfs: "+signature);
    LinkedList<HierarchySignature> queue = new LinkedList<HierarchySignature>();
    queue.add(new HierarchySignature(signature));
    queue.add(new HierarchySignature("<java.lang.Integer: java.lang.String toString(int)>"));
    queue.add(new HierarchySignature("<java.lang.Long: java.lang.String toString(long)>"));
    queue.add(new HierarchySignature("<edu.syr.pcpratts.rootbeer.runtime.Sentinal: void <init>()>"));
    queue.add(new HierarchySignature("<edu.syr.pcpratts.rootbeer.runtimegpu.GpuException: void <init>()>"));
    queue.add(new HierarchySignature("<edu.syr.pcpratts.rootbeer.runtimegpu.GpuException: edu.syr.pcpratts.rootbeer.runtimegpu.GpuException arrayOutOfBounds(int,int,int)>"));
    
    visited.add(new HierarchySignature("<java.lang.StringBuilder: java.lang.StringBuilder append(double)>"));
    visited.add(new HierarchySignature("<java.lang.StringBuilder: java.lang.StringBuilder append(float)>"));
    
    while(queue.isEmpty() == false){
      HierarchySignature curr = queue.removeFirst();
      doDfsForRootbeer(curr, queue, visited);
    }
  }

  private void doDfsForRootbeer(HierarchySignature signature, 
    LinkedList<HierarchySignature> queue, Set<HierarchySignature> visited){

    if(visited.contains(signature)){
      return;
    }
    visited.add(signature);
        
    StringToType converter = new StringToType();
    FieldSignatureUtil futil = new FieldSignatureUtil();
    
    m_currDfsInfo.addType(signature.getClassName());
    m_currDfsInfo.addType(signature.getReturnType());
    m_currDfsInfo.addMethod(signature.toString());
    
    ClassHierarchy class_hierarchy = RootbeerClassLoader.v().getClassHierarchy();
    List<HierarchySignature> virt_methods = class_hierarchy.getVirtualMethods(signature);
    for(HierarchySignature virt_method : virt_methods){
      if(RootbeerClassLoader.v().dontFollow(virt_method)){
        continue;
      }

      if(virt_method.equals(signature) == false){
    	  queue.add(virt_method);
      }
    }

    HierarchyValueSwitch value_switch = RootbeerClassLoader.v().getValueSwitch(signature);
    for(Integer num : value_switch.getAllTypesInteger()){
      String type_str = StringNumbers.v().getString(num);
      Type type = converter.convert(type_str);
      m_currDfsInfo.addType(type);
    }    

    for(HierarchySignature method_sig : value_switch.getMethodRefsHierarchy()){
      m_currDfsInfo.addMethod(signature.toString());
      
      if(RootbeerClassLoader.v().dontFollow(method_sig)){
        continue;
      }
         	      
      queue.add(method_sig);
    }

    for(String field_ref : value_switch.getFieldRefs()){
      futil.parse(field_ref);
      SootField soot_field = futil.getSootField();
      m_currDfsInfo.addField(soot_field);
    }

    for(Integer num : value_switch.getInstanceOfsInteger()){
      String type_str = StringNumbers.v().getString(num);
      Type type = converter.convert(type_str);
      m_currDfsInfo.addInstanceOf(type);
    }
  }
}