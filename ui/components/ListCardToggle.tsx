import * as React from 'react';
import { Icon } from 'antd';
import Colors from './Colors';

interface Props {
  selectedMode: string;
  style?: any;
  onSelect?: (mode: string) => void;
}

const toggles = [{ text: 'list', icon: 'bars' }, { text: 'cards', icon: 'appstore' }];

const ListCardToggle = ({ selectedMode, style = {}, onSelect }: Props) => {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'flex-end',
        ...style,
      }}
    >
      {toggles.map(({ text, icon }, index) => (
        <div
          key={text}
          style={{
            textAlign: 'center',
            width: 36,
            color: Colors.PrimaryColor.string(),
            borderBottomWidth: 1,
            borderBottomStyle: 'solid',
            borderBottomColor: selectedMode === text ? Colors.PrimaryColor.string() : 'transparent',
            paddingBottom: 4,
            margin: '0 4px',
            cursor: 'pointer',
          }}
          onClick={() => !!onSelect && onSelect(text)}
        >
          <Icon type={icon} style={{ fontSize: '20px' }} />
          <span
            style={{
              display: 'block',
              fontSize: '10px',
              lineHeight: '10px',
              textTransform: 'uppercase',
            }}
          >
            {text}
          </span>
        </div>
      ))}
    </div>
  );
};

export default ListCardToggle;
