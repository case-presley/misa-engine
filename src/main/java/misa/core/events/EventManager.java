package misa.core.events;

import misa.scripting.LuaEventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class EventManager
{
    private final Map<Class<? extends Event>, List<EventListener<? extends Event>>> eventListeners;
    private final LuaEventHandler luaEventHandler;

    public EventManager(LuaEventHandler luaEventHandler)
    {
        this.eventListeners = new HashMap<>();
        this.luaEventHandler = luaEventHandler;
    }

    public <T extends Event> void addListener(Class<T> eventType, EventListener<T> listener)
    {
        eventListeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void addLuaListener(String eventName, String luaScript)
    {
        luaEventHandler.registerEventHandler(eventName, luaScript);
    }

    public <T extends Event> void removeListener(Class<T> eventType, EventListener<T> listener)
    {
        List<EventListener<? extends Event>> listeners = eventListeners.get(eventType);
        if (listeners != null) listeners.remove(listener);
    }

    public <T extends Event> void triggerEvent(T event)
    {
        List<EventListener<? extends Event>> listeners = eventListeners.get(event.getClass());
        if (listeners != null)
        {
            for (EventListener<? extends Event> listener : listeners)
            {
                @SuppressWarnings("unchecked")
                EventListener<T> typedListener = (EventListener<T>) listener;
                typedListener.handleEvent(event);
            }
        }
    }
}