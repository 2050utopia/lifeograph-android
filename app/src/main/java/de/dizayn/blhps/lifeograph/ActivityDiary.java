/***********************************************************************************

    Copyright (C) 2012-2013 Ahmet Öztürk (aoz_2@yahoo.com)

    This file is part of Lifeograph.

    Lifeograph is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Lifeograph is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Lifeograph.  If not, see <http://www.gnu.org/licenses/>.

 ***********************************************************************************/

package de.dizayn.blhps.lifeograph;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
//import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.dizayn.blhps.lifeograph.DiaryElement.Type;

public class ActivityDiary extends ListActivity {
    private LayoutInflater mInflater;

    public static Entry entry_current = null;
    protected DiaryElement mParentElem = null;

    protected ActionBar mActionBar = null;

    static protected ElemListAllEntries mElemAllEntries = null;

    protected boolean mFlagSaveOnLogOut = true;
    protected boolean mFlagLogoutOnPause = false;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        Lifeobase.activityDiary = this;

        if( Diary.diary == null )
            Diary.diary = new Diary();

        setContentView( R.layout.diary );

        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled( true );
        mInflater = ( LayoutInflater ) getSystemService( Activity.LAYOUT_INFLATER_SERVICE );

        m_adapter_entries = new DiaryElemAdapter( this, R.layout.imagelist, R.id.title, m_elems );
        this.setListAdapter( m_adapter_entries );
        update_entry_list();

        mFlagSaveOnLogOut = true;
        Log.d( "LFO", "RESET MFLAGSAVEONLOGOUT" );
    }

    @Override
    public void onPause() {
        super.onPause();
        if( mFlagLogoutOnPause )
            handleLogout(); // TODO: save backup if not successful
        Log.d( "LFO", "onPause - ActivityDiary" );
    }

    /* onDestroy is called after next Activity gets started so this is not useful
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d( "LFO", "onDestroy - ActivityDiary" );
    }*/

    @Override
    public void onResume() {
        super.onResume();
        mFlagLogoutOnPause = false;
        //update_entry_list();
        Log.i( "L", "onResume - ActivityDiary" );
    }

    boolean handleLogout() {
        // SAVING
        // sync_entry();

        // Diary.diary.m_last_elem = get_cur_elem()->get_id();

        if( mFlagSaveOnLogOut ) {
            if( Diary.diary.write() != Result.SUCCESS ) {
                Toast.makeText( Lifeobase.activityOpenDiary, "Cannot write back changes",
                                Toast.LENGTH_LONG ).show();
                return false;
            }
            else {
                // ActivityDiary.this.finish();
                Toast.makeText( Lifeobase.activityOpenDiary, "Diary saved successfully",
                                Toast.LENGTH_LONG ).show();
                return true;
            }
        }
        else {
            Log.d( "LFO", "Logged out without saving" );
            return true;
        }
    }

    public void showEntry( Entry entry ) {
        if( entry != null ) {
            entry_current = entry;
            Intent i = new Intent( this, ActivityEntry.class );
            startActivity( i );
        }
    }

    public void goToToday() {
        Entry entry = Diary.diary.get_entry_today();

        if( entry == null ) // add new entry if no entry exists on selected date
        {
            entry = Diary.diary.add_today();
            // update_entry_list();
        }

        showEntry( entry );
    }

    public void showCalendar() {
        // Intent i = new Intent( this, ActivityCalendar.class );
        // startActivityForResult( i, ActivityCalendar.REQC_OPEN_ENTRY );
        DialogCalendar dialog = new DialogCalendar( this );
        dialog.show();
    }

    // @Override
    // protected void onActivityResult( int reqCode, int resultCode, Intent data ) {
    // super.onActivityResult( reqCode, resultCode, data );
    // if( resultCode == ActivityCalendar.REQC_OPEN_ENTRY ) {
    // changeView();
    // }
    // else {
    // Toast.makeText( this, "Fail", Toast.LENGTH_LONG ).show();
    // }
    // }

    @Override
    public void onBackPressed() {
        if( mParentElem == null ) {
            mFlagLogoutOnPause = true;
            super.onBackPressed();
        }
        else {
            mParentElem = null;
            update_entry_list();
        }
        return;
    }

    @Override
    protected void onListItemClick( ListView l, View v, int pos, long id ) {
        super.onListItemClick( l, v, pos, id );
        switch( m_elems.get( pos ).get_type() ) {
            case ENTRY:
                showEntry( ( Entry ) m_elems.get( pos ) );
                break;
            case ALLBYDATE:
                mParentElem = mElemAllEntries;
                update_entry_list();
                break;
            case TAG:
            case CHAPTER:
            case TOPIC:
                mParentElem = m_elems.get( pos );
                update_entry_list();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        super.onPrepareOptionsMenu( menu );

        MenuItem item = menu.findItem( R.id.today );
        item.setVisible( mParentElem == null );

        item = menu.findItem( R.id.calendar );
        item.setVisible( mParentElem == null );

        item = menu.findItem( R.id.add_topic );
        item.setVisible( mParentElem == null );

        item = menu.findItem( R.id.change_sort_type );
        item.setVisible( mParentElem != null );

        DiaryElement.Type type = DiaryElement.Type.NONE;
        if( mParentElem != null )
            type = mParentElem.get_type();

        item = menu.findItem( R.id.add_entry );
        item.setVisible( type == DiaryElement.Type.TOPIC );

        item = menu.findItem( R.id.dismiss );
        item.setVisible( mParentElem != null );

        item = menu.findItem( R.id.rename );
        item.setVisible( mParentElem != null );

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );

        getMenuInflater().inflate( R.menu.menu_diary, menu );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() )
        {
            case android.R.id.home:
                if( mParentElem == null ) {
                    mFlagLogoutOnPause = true;
                    //NavUtils.navigateUpFromSameTask( this );
                    finish();
                }
                else {
                    mParentElem = null;
                    update_entry_list();
                }
                return true;
            case R.id.today:
                goToToday();
                return true;
            case R.id.calendar:
                showCalendar();
                return true;
            case R.id.add_topic:
                create_topic();
                return true;
            case R.id.add_entry:
                showEntry( Diary.diary.create_entry( ( ( Chapter ) mParentElem ).get_free_order(),
                        "", false ) );
                return true;
            case R.id.rename:
                switch( mParentElem.get_type() ) {
                    case TAG:
                        rename_tag();
                        break;
                    case CHAPTER:
                    case TOPIC:
                        rename_chapter();
                        break;
                    default:
                        break;
                }
                return true;
            case R.id.dismiss:
                switch( mParentElem.get_type() ) {
                    case TAG:
                        dismiss_tag();
                        break;
                    case CHAPTER:
                    case TOPIC:
                        dismiss_chapter();
                        break;
                    default:
                        break;
                }
                return true;
            case R.id.logout_wo_save:
                AlertDialog.Builder builder = new AlertDialog.Builder( this );
                builder.setMessage( R.string.logoutwosaving_confirm )
                        .setCancelable( false )
                        .setPositiveButton( R.string.logoutwosaving,
                                new DialogInterface.OnClickListener() {
                                    public void onClick( DialogInterface dialog, int id ) {
                                        // unlike desktop version Android version
                                        // does
                                        // not back up changes
                                        mFlagSaveOnLogOut = false;
                                        ActivityDiary.this.finish();
                                    }
                                } )
                        .setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick( DialogInterface dialog, int id ) {
                                mFlagSaveOnLogOut = true;
                                dialog.cancel();
                            }
                        } );
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            case R.id.import_sms:
                import_messages();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void update_entry_list() {
        m_adapter_entries.clear();
        m_elems.clear();
        DiaryElement.Type type = mParentElem == null ? Type.NONE : mParentElem.get_type();
        switch( type ) {
            case NONE:
                mActionBar.setIcon( R.drawable.ic_diary );
                setTitle( Diary.diary.get_name() );
                mActionBar.setSubtitle( "Diary with " + Diary.diary.m_entries.size() + " Entries" );

                if( mElemAllEntries == null ) {
                    mElemAllEntries = new ElemListAllEntries( Diary.diary );
                }
                m_elems.add( mElemAllEntries );

                for( Chapter c : Diary.diary.m_custom_sorteds.mMap.values() ) {
                    m_elems.add( c );
                }
                for( Chapter c : Diary.diary.m_topics.mMap.values() ) {
                    m_elems.add( c );
                }
                for( Chapter c : Diary.diary.m_ptr2chapter_ctg_cur.mMap.values() ) {
                    m_elems.add( c );
                }
                for( Tag t : Diary.diary.m_tags.values() ) {
                    m_elems.add( t );
                }
                for( Entry e : Diary.diary.m_orphaned_entries ) {
                    m_elems.add( e );
                }
                break;
            case TAG:
                mActionBar.setIcon( mParentElem.get_icon() );
                setTitle( mParentElem.getListStr() );
                mActionBar.setSubtitle( mParentElem.getSubStr() );

                Tag t = ( Tag ) mParentElem;
                for( Entry e : t.mEntries ) {
                    m_elems.add( e );
                }
                break;
            case CHAPTER:
            case TOPIC:
            case SORTED:
                mActionBar.setIcon( mParentElem.get_icon() );
                setTitle( mParentElem.getListStr() );
                mActionBar.setSubtitle( mParentElem.getSubStr() );

                Chapter c = ( Chapter ) mParentElem;
                for( Entry e : c.mEntries ) {
                    m_elems.add( e );
                }
                break;
            case ALLBYDATE:
                for( Entry e : Diary.diary.m_entries.values() ) {
                    m_elems.add( e );
                }
                break;
            default:
                break;
        }

        // force menu update
        invalidateOptionsMenu();
    }

    protected void create_topic() {
        final EditText input = new EditText( this );
        input.setText( "New topic" );
        AlertDialog.Builder dlg = new AlertDialog.Builder( this );
        dlg.setTitle( "Create Topic" )
                // .setMessage(message)
                .setView( input )
                .setPositiveButton( R.string.create_topic, new DialogInterface.OnClickListener()
                {
                    public void onClick( DialogInterface di, int btn )
                    {
                        mParentElem = Diary.diary.m_topics.create_chapter_ordinal(
                                input.getText().toString() );
                        Diary.diary.update_entries_in_chapters();
                        update_entry_list();
                    }
                } )
                .setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick( DialogInterface di, int btn )
                    {
                    } // do nothing
                } ).show();
    }
    protected void create_sorted() {
        final EditText input = new EditText( this );
        input.setText( "New group" );
        AlertDialog.Builder dlg = new AlertDialog.Builder( this );
        dlg.setTitle( "Create Group" )
                // .setMessage(message)
                .setView( input )
                .setPositiveButton( R.string.create_group, new DialogInterface.OnClickListener()
                {
                    public void onClick( DialogInterface di, int btn )
                    {
                        mParentElem = Diary.diary.m_custom_sorteds.create_chapter_ordinal(
                                input.getText().toString() );
                        Diary.diary.update_entries_in_chapters();
                        update_entry_list();
                    }
                } )
                .setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick( DialogInterface di, int btn )
                    {
                    } // do nothing
                } ).show();
    }

    protected void rename_tag() {
        final EditText input = new EditText( this );
        input.setText( mParentElem.m_name );
        AlertDialog.Builder dlg = new AlertDialog.Builder( this );
        dlg.setTitle( "Rename Tag" )
           // .setMessage(message)
           .setView( input )
           .setPositiveButton( R.string.rename, new DialogInterface.OnClickListener()
           {
               public void onClick( DialogInterface di, int btn )
               {
                   Diary.diary.rename_tag( ( Tag ) mParentElem, input.getText().toString() );
                   setTitle( mParentElem.m_name );
               }
           } ).setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
               public void onClick( DialogInterface di, int btn ) {
                   // do nothing
               }
           } ).show();
    }

    protected void rename_chapter() {
        final EditText input = new EditText( this );
        input.setText( mParentElem.m_name );
        // input.selectAll();
        AlertDialog.Builder dlg = new AlertDialog.Builder( this );
        dlg.setTitle( "Rename Chapter/Topic" )
           // .setMessage(message)
           .setView( input )
           .setPositiveButton( R.string.rename, new DialogInterface.OnClickListener()
           {
               public void onClick( DialogInterface di, int btn )
               {
                   ( ( Chapter ) mParentElem ).m_name = input.getText().toString();
                   setTitle( mParentElem.m_name );
               }
           } ).setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
               public void onClick( DialogInterface di, int btn ) {
                   // do nothing
               }
           } ).show();
    }

    protected void dismiss_chapter() {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( R.string.chapter_dismiss_confirm )
               .setPositiveButton( R.string.dismiss, new DialogInterface.OnClickListener() {
                   public void onClick( DialogInterface dialog, int id ) {
                       Diary.diary.dismiss_chapter( ( Chapter ) mParentElem );
                       // go up:
                       mParentElem = null;
                       Diary.diary.update_entries_in_chapters();
                       update_entry_list();
                   }
               } ).setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick( DialogInterface dialog, int id ) {
                       dialog.cancel();
                   }
               } ).show();
    }

    protected void dismiss_tag() {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( R.string.tag_dismiss_confirm )
               .setPositiveButton( R.string.dismiss, new DialogInterface.OnClickListener() {
                   public void onClick( DialogInterface dialog, int id ) {
                       Diary.diary.dismiss_tag( ( Tag ) mParentElem );
                       // go up:
                       mParentElem = null;
                       update_entry_list();
                   }
               } ).setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick( DialogInterface dialog, int id ) {
                       dialog.cancel();
                   }
               } ).show();
    }

    protected void import_messages() {
        Cursor cursor =
                getContentResolver().query( Uri.parse( "content://sms/inbox" ), null, null, null,
                                            null );
        cursor.moveToFirst();

        do {
            String body = new String();
            Calendar cal = Calendar.getInstance();

            for( int idx = 0; idx < cursor.getColumnCount(); idx++ ) {
                String msgData = cursor.getColumnName( idx );

                if( msgData.compareTo( "body" ) == 0 )
                    body = cursor.getString( idx );
                else if( msgData.compareTo( "date" ) == 0 )
                    cal.setTimeInMillis( cursor.getLong( idx ) );
            }

            Diary.diary.create_entry( new Date( cal.get( Calendar.YEAR ),
                                                cal.get( Calendar.MONTH ) + 1,
                                                cal.get( Calendar.DAY_OF_MONTH ) ), body, false );
        }
        while( cursor.moveToNext() );

    }

    private class ElemListAllEntries extends DiaryElement {
        int mSize = 0;

        public ElemListAllEntries( Diary diary ) {
            super( diary, getString( R.string.all_entries ), ES_VOID );
            mType = Type.ALLBYDATE;
        }

        @Override
        public String getSubStr() {
            // TODO Auto-generated method stub
            return( mType == Type.ALLBYDATE ? getString( R.string.sort_by_date ) : getString( R.string.sort_by_size ) );
        }

        @Override
        public int get_icon() {
            // TODO Auto-generated method stub
            return R.drawable.ic_diary;
        }

        protected Type mType;

        @Override
        public Type get_type() {
            return mType;
        }

        @Override
        public int get_size() {
            return mSize;
        }

    }

    protected java.util.List< DiaryElement > m_elems = new ArrayList< DiaryElement >();
    protected DiaryElemAdapter m_adapter_entries;

    private class DiaryElemAdapter extends ArrayAdapter< DiaryElement > {

        public DiaryElemAdapter( Context context, int resource, int textViewResourceId,
                                 java.util.List< DiaryElement > objects ) {
            super( context, resource, textViewResourceId, objects );
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            ViewHolder holder;
            TextView title;
            TextView detail;
            ImageView icon;
            ImageView icon2;
            DiaryElement elem = getItem( position );

            if( convertView == null ) {
                convertView = mInflater.inflate( R.layout.imagelist, null );
                holder = new ViewHolder( convertView );
                convertView.setTag( holder );
            }
            holder = ( ViewHolder ) convertView.getTag();

            title = holder.getName();
            title.setText( elem.getListStr() );

            detail = holder.getDetail();
            detail.setText( elem.getListStrSecondary() );

            icon = holder.getIcon();
            icon.setImageResource( elem.get_icon() );

            icon2 = holder.getIcon2();
            icon2.setImageResource( R.drawable.ic_favorite );
            icon2.setVisibility( elem.is_favored() ? View.VISIBLE : View.INVISIBLE );

            return convertView;
        }

        private class ViewHolder {
            private View mRow;
            private TextView title = null;
            private TextView detail = null;
            private ImageView icon = null;
            private ImageView icon2 = null;

            public ViewHolder( View row ) {
                mRow = row;
            }

            public TextView getName() {
                if( null == title ) {
                    title = ( TextView ) mRow.findViewById( R.id.title );
                }
                return title;
            }

            public TextView getDetail() {
                if( null == detail ) {
                    detail = ( TextView ) mRow.findViewById( R.id.detail );
                }
                return detail;
            }

            public ImageView getIcon() {
                if( null == icon ) {
                    icon = ( ImageView ) mRow.findViewById( R.id.icon );
                }
                return icon;
            }

            public ImageView getIcon2() {
                if( null == icon2 ) {
                    icon2 = ( ImageView ) mRow.findViewById( R.id.icon2 );
                }
                return icon2;
            }
        }
    }
}
