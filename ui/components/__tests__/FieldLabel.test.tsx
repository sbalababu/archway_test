import * as React from 'react';
import { shallow } from 'enzyme';
import toJson from 'enzyme-to-json';

import FieldLabel from '../FieldLabel';

describe('FieldLabel', () => {
  it('renders correctly', () => {
    const wrapper = shallow(<FieldLabel children={null} />);
    expect(toJson(wrapper)).toMatchSnapshot();
  });
});
