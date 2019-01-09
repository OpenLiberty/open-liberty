/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.faces.component.behavior;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.PartialStateHolder;
import javax.faces.component.StateHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.BehaviorEvent;
import javax.faces.event.BehaviorListener;

/**
 * @since 2.0
 */
public class BehaviorBase implements Behavior, PartialStateHolder
{
    private _DeltaList<BehaviorListener> _behaviorListeners;
    
    private boolean _initialState;
    
    private transient boolean _transient;

    /**
     * 
     */
    public BehaviorBase()
    {
    }
    
    //public abstract String getRendererType();
    /**
     * {@inheritDoc}
     */
    public void broadcast(BehaviorEvent event) throws AbortProcessingException
    {
        if (event == null)
        {
            throw new NullPointerException("event");
        }
        
        if (_behaviorListeners != null)
        {
            // This code prevent listeners from unregistering themselves while processing the event.
            // I believe it should always be alright in this case. However, the need rise, then it 
            // should be possible to remove that limitation by using a clone for the looping
            for (int i = 0; i < _behaviorListeners.size() ; i++)
            {
                BehaviorListener listener = _behaviorListeners.get(i);
                if (event.isAppropriateListener(listener))
                {
                    event.processListener(listener);
                }
            }
        }
    }

    public void clearInitialState()
    {
        _initialState = false;
        if (_behaviorListeners != null)
        {
            _behaviorListeners.clearInitialState();
        }
    }

    public boolean initialStateMarked()
    {
        return _initialState;
    }

    public boolean isTransient()
    {
        return _transient;
    }

    public void markInitialState()
    {
        _initialState = true;
        if (_behaviorListeners != null)
        {
            _behaviorListeners.markInitialState();
        }
    }

    public void restoreState(FacesContext context, Object state)
    {
        if (state == null)
        {
            return;
        }
        else if (state instanceof _AttachedDeltaWrapper)
        {
            //Delta: check for null is not necessary since _behaviorListener field
            //is only set once and never reset
            //if (_behaviorListeners != null)
            //{
                ((StateHolder)_behaviorListeners).restoreState(context,
                        ((_AttachedDeltaWrapper) state).getWrappedStateObject());
            //}
        }
        else
        {
            //Full
            _behaviorListeners = (_DeltaList<BehaviorListener>)
                restoreAttachedState(context, state);
        }
    }

    public Object saveState(FacesContext context)
    {
        return saveBehaviorListenersList(context);
    }
    
    private Object saveBehaviorListenersList(FacesContext facesContext)
    {
        PartialStateHolder holder = (PartialStateHolder) _behaviorListeners;
        if (initialStateMarked() && _behaviorListeners != null && holder.initialStateMarked())
        {                
            Object attachedState = holder.saveState(facesContext);
            if (attachedState != null)
            {
                return new _AttachedDeltaWrapper(_behaviorListeners.getClass(),
                        attachedState);
            }
            //_behaviorListeners instances once is created never changes, we can return null
            return null;
        }
        else
        {
            return saveAttachedState(facesContext,_behaviorListeners);
        }
    }

    private static Object saveAttachedState(FacesContext context, Object attachedObject)
    {
        if (context == null)
        {
            throw new NullPointerException ("context");
        }
        
        if (attachedObject == null)
        {
            return null;
        }
        // StateHolder interface should take precedence over
        // List children
        if (attachedObject instanceof StateHolder)
        {
            StateHolder holder = (StateHolder) attachedObject;
            if (holder.isTransient())
            {
                return null;
            }

            return new _AttachedStateWrapper(attachedObject.getClass(), holder.saveState(context));
        }        
        else if (attachedObject instanceof List)
        {
            List<Object> lst = new ArrayList<Object>(((List<?>) attachedObject).size());
            for (Object item : (List<?>) attachedObject)
            {
                if (item != null)
                {
                    lst.add(saveAttachedState(context, item));
                }
            }

            return new _AttachedListStateWrapper(lst);
        }
        else if (attachedObject instanceof Serializable)
        {
            return attachedObject;
        }
        else
        {
            return new _AttachedStateWrapper(attachedObject.getClass(), null);
        }
    }

    private static Object restoreAttachedState(FacesContext context, Object stateObj) throws IllegalStateException
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        if (stateObj == null)
        {
            return null;
        }
        if (stateObj instanceof _AttachedListStateWrapper)
        {
            List<Object> lst = ((_AttachedListStateWrapper) stateObj).getWrappedStateList();
            List<Object> restoredList = new ArrayList<Object>(lst.size());
            for (Object item : lst)
            {
                restoredList.add(restoreAttachedState(context, item));
            }
            return restoredList;
        }
        else if (stateObj instanceof _AttachedStateWrapper)
        {
            Class<?> clazz = ((_AttachedStateWrapper) stateObj).getClazz();
            Object restoredObject;
            try
            {
                restoredObject = clazz.newInstance();
            }
            catch (InstantiationException e)
            {
                throw new RuntimeException("Could not restore StateHolder of type " + clazz.getName()
                        + " (missing no-args constructor?)", e);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
            if (restoredObject instanceof StateHolder)
            {
                _AttachedStateWrapper wrapper = (_AttachedStateWrapper) stateObj;
                Object wrappedState = wrapper.getWrappedStateObject();

                StateHolder holder = (StateHolder) restoredObject;
                holder.restoreState(context, wrappedState);
            }
            return restoredObject;
        }
        else
        {
            return stateObj;
        }
    }

    public void setTransient(boolean newTransientValue)
    {
        _transient = newTransientValue;
    }
    
    protected void addBehaviorListener(BehaviorListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("listener");
        }
        
        if (_behaviorListeners == null)
        {
            // Lazy instanciation with size 1:
            // the only posibility how to add listener is <f:ajax listener="" /> - there is no <f:ajaxListener/> tag 
            _behaviorListeners = new _DeltaList<BehaviorListener>(1);
        }
        
        _behaviorListeners.add(listener);
    }
    
    protected void removeBehaviorListener(BehaviorListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("listener");
        }

        if (_behaviorListeners != null)
        {
            _behaviorListeners.remove(listener);
        }
    }
}
