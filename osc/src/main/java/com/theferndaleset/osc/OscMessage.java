package com.theferndaleset.osc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mike on 6/12/2018.
 */

public class OscMessage {
    private String _address;
    private List<Object> _arguments;

    public OscMessage (ByteBuffer bytes) throws UnsupportedEncodingException {
        _arguments = new LinkedList<Object>();
        int typeStringOffset = -1;
        int argumentOffset = -1;

        int nullCount = 0;
        for (int i = 3; i < bytes.capacity() && nullCount < 2; i=i+4)
        {
            if (bytes.get(i) == 0x00)
            {
                nullCount++;
                if (typeStringOffset == -1)
                    typeStringOffset = i+1;
                else if (argumentOffset == -1)
                    argumentOffset = i+1;
            }
        }
        if (typeStringOffset > 0){
                byte[] addressBytes = Arrays.copyOfRange(bytes.array(), 0, typeStringOffset);
                _address = new String(addressBytes, "UTF8").trim();

                int argValueIndex = 0;
                for (int i = typeStringOffset+1; i < bytes.capacity(); i++) {
                    switch(bytes.get(i))
                    {
                        case 0x66: // f
                            _arguments.add(bytes.getFloat(argumentOffset+argValueIndex));
                            argValueIndex += 4;
                            break;
                        case 0x69: // i
                            _arguments.add(bytes.getInt(argumentOffset+argValueIndex));
                            argValueIndex += 4;
                            break;
                        case 0x73: // s
                            int terminationIndex = argumentOffset+argValueIndex;
                            for (int sIndex = argumentOffset+argValueIndex; sIndex < bytes.capacity(); sIndex++) {
                                if (bytes.get(sIndex) == 0x00) {
                                    terminationIndex = sIndex;
                                    break;
                                }
                            }
                            byte[] valueBytes = Arrays.copyOfRange(bytes.array(), argumentOffset+argValueIndex, terminationIndex);
                            _arguments.add(new String(valueBytes, "UTF=8").trim());
                            argValueIndex += (terminationIndex - (argumentOffset+argValueIndex)) + (4-((terminationIndex - (argumentOffset+argValueIndex))%4));
                            break;
                        case 0x00: // null
                            i = bytes.capacity() + 1;
                            break;
                    }
                }
        }


    }
    public OscMessage(String address) {
        _address = padd(address);
        _arguments = new LinkedList<Object>();
    }

    public OscMessage(String address, Object[] arguments) {
        _address = padd(address);
        _arguments = new LinkedList<Object>();

        for(Object o : arguments)
            _arguments.add(o);
    }

    public void addArgument(Object o)
    {
        _arguments.add(o);
    }

    public <E> E getArgument(int index)
    {
        return (E)_arguments.get(index);
    }
    public int getArgumentCount() {
        return _arguments.size();
    }

    public String getAddress() {
        return _address;
    }

    public byte[] getBytes() {
        try {
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            ByteArrayOutputStream argStream = new ByteArrayOutputStream();

            String typeString = ",";
            for (Object o : _arguments) {
                typeString = typeString.concat(getTypeString(o));
                byte argBytes[] = getByteArray(o);
                argStream.write(argBytes);
            }

            typeString = padd(typeString);

            resultStream.write(_address.getBytes(Charset.forName("UTF8")));
            resultStream.write(typeString.getBytes(Charset.forName("UTF8")));
            resultStream.write(argStream.toByteArray());

            return resultStream.toByteArray();
        } catch(IOException io)
        {
            return null;
        }
    }

    private String padd(String s) {
        s = s.concat("\0");
        int padding = 4-(s.length()%4);
        if (padding == 4)
            padding = 0;

        for (int i = 0; i < padding; i++)
            s = s.concat("\0");

        return s;
    }

    private String getTypeString(Object o)
    {
        if (o instanceof Integer)
            return "i";
        else if (o instanceof Float)
            return "f";
        else if (o instanceof String)
            return "s";
        else return "";
    }

    private byte[] getByteArray(Object o)
    {
        if (o instanceof Integer) {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putInt((Integer) o);
            return bb.array();
        }
        else if (o instanceof Float){
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putFloat((Float) o);
            return bb.array();
        }
        else if (o instanceof String){
            String s = padd((String)o);
            return s.getBytes(Charset.forName("UTF8"));
        }
        else return null;
    }
}
